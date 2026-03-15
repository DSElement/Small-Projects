/*
gcc -g raft_mpi.c -I "c:\Program Files (x86)\Microsoft SDKs\MPI\Include" -L "c:\Program Files (x86)\Microsoft SDKs\MPI\Lib\x64" -lmsmpi -o raft_mpi.exe

mpiexec -n 7 raft_mpi.exe
*/

#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>

#define MAGIC_NUMBER_SPREAD 1337
#define MAX_NODES 7
#define HEARTBEAT_SEC 0.05

#define DISABLE_AFTER_SEC  3.0
#define ENABLE_AFTER_SEC   7.0

typedef long int raft_term_t;
typedef int      raft_node_id_t;

typedef enum {
    STATE_NONE = 0,
    STATE_FOLLOWER,
    STATE_CANDIDATE,
    STATE_LEADER
} raft_state_t;

typedef struct {
    raft_term_t term;
    raft_node_id_t candidate_id;
} msg_requestvote_t;

typedef struct {
    raft_term_t term;
    int vote_granted;
} msg_requestvote_response_t;

typedef struct {
    raft_term_t term;
    raft_node_id_t leader_id;
} msg_heartbeat_t;

typedef struct {
    raft_term_t term;
} msg_heartbeat_response_t;

enum {
    TAG_REQUEST_VOTE      = 100,
    TAG_REQUEST_VOTE_RESP = 101,
    TAG_HEARTBEAT         = 102,
    TAG_HEARTBEAT_RESP    = 103
};

typedef struct {
    int id;
    raft_state_t state;

    raft_term_t currentTerm;
    int votedFor;
    int votes_received;
    int vote_seen[MAX_NODES + 1];

    int timeout_ms;
    double last_received_time;
    double next_heartbeat_time;

    int enabled;   
} raft_node_t;

static int random_timeout_ms(void) {
    return 150 + rand() % 151; // [150, 300]
}

static void broadcast_request_vote(int world_size, const msg_requestvote_t* msg) {
    int k = 0;
    for (int p = 0; p < world_size; p++) {
        if (p == msg->candidate_id) continue;
        k++;
    }
    if (k == 0) return;

    MPI_Request *reqs = (MPI_Request*)malloc((size_t)k * sizeof(*reqs));
    if (!reqs) return;

    int i = 0;
    for (int p = 0; p < world_size; p++) {
        if (p == msg->candidate_id) continue;
        MPI_Isend((void*)msg, (int)sizeof(*msg), MPI_BYTE, p,
                  TAG_REQUEST_VOTE, MPI_COMM_WORLD, &reqs[i++]);
    }

    MPI_Status *stats = (MPI_Status*)malloc((size_t)k * sizeof(*stats));
    if (!stats) {
        perror("malloc");
        free(reqs);
        exit(EXIT_FAILURE);
    }

    MPI_Waitall(k, reqs, stats);

    free(stats);
    free(reqs);
}

static void send_request_vote_response(int dest, const msg_requestvote_response_t* resp) {
    MPI_Request r;
    MPI_Isend((void*)resp, (int)sizeof(*resp), MPI_BYTE, dest, TAG_REQUEST_VOTE_RESP, MPI_COMM_WORLD, &r);
    MPI_Wait(&r, MPI_STATUS_IGNORE);
}

static void send_heartbeat_response(int dest, const msg_heartbeat_response_t* resp) {
    MPI_Request r;
    MPI_Isend((void*)resp, (int)sizeof(*resp), MPI_BYTE, dest, TAG_HEARTBEAT_RESP, MPI_COMM_WORLD, &r);
    MPI_Wait(&r, MPI_STATUS_IGNORE);
}

static void handle_request_vote(raft_node_t* node, const msg_requestvote_t* rv) {
    msg_requestvote_response_t resp;
    resp.vote_granted = 0;

    if (rv->term > node->currentTerm) {
        node->currentTerm = rv->term;
        node->state = STATE_FOLLOWER;
        node->votedFor = -1;
    }

    if (rv->term == node->currentTerm) {
        if (node->votedFor == -1 || node->votedFor == rv->candidate_id) {
            node->votedFor = rv->candidate_id;
            resp.vote_granted = 1;
            node->last_received_time = MPI_Wtime();
        }
    }

    resp.term = node->currentTerm;

    printf("[Node %d] Received RequestVote from %d (term=%ld) -> %s (votedFor=%d, myTerm=%ld)\n",
           node->id, rv->candidate_id, (long)rv->term,
           resp.vote_granted ? "GRANT" : "DENY",
           node->votedFor, (long)node->currentTerm);
    fflush(stdout);

    send_request_vote_response(rv->candidate_id, &resp);
}

static void handle_request_vote_response(raft_node_t* node,
                                         const msg_requestvote_response_t* r,
                                         int sender,
                                         int world_size) {
    if (r->term > node->currentTerm) {
        node->currentTerm = r->term;
        node->state = STATE_FOLLOWER;
        node->votedFor = -1;
        node->votes_received = 0;
        printf("[Node %d] Saw higher term in vote response -> step down to FOLLOWER (term=%ld)\n",
               node->id, (long)node->currentTerm);
        fflush(stdout);
        return;
    }

    if (node->state != STATE_CANDIDATE) return;

    if (r->vote_granted) {
        if (sender >= 0 && sender <= MAX_NODES && node->vote_seen[sender]) return;
        if (sender >= 0 && sender <= MAX_NODES) node->vote_seen[sender] = 1;

        node->votes_received++;
        int majority = (world_size / 2) + 1;

        printf("[Node %d] Vote granted from %d! votes_received=%d (need %d)\n",
               node->id, sender, node->votes_received, majority);
        fflush(stdout);

        if (node->votes_received >= majority) {
            node->state = STATE_LEADER;
            node->next_heartbeat_time = MPI_Wtime();
            printf("[Node %d] >>> ELECTED LEADER for term %ld <<<\n",
                   node->id, (long)node->currentTerm);
            fflush(stdout);
        }
    }
}

static void handle_heartbeat(raft_node_t* node, const msg_heartbeat_t* hb) {
    if (hb->term >= node->currentTerm) {
        if (hb->term > node->currentTerm) {
            node->currentTerm = hb->term;
            node->votedFor = -1;
        }
        node->state = STATE_FOLLOWER;
        node->last_received_time = MPI_Wtime();
    }
}

static void handle_heartbeat_response(raft_node_t* node, const msg_heartbeat_response_t* r) {
    if (r->term > node->currentTerm) {
        node->currentTerm = r->term;
        node->state = STATE_FOLLOWER;
        node->votedFor = -1;
        node->votes_received = 0;
        printf("[Node %d] HeartbeatResp has higher term -> step down to FOLLOWER (term=%ld)\n",
               node->id, (long)node->currentTerm);
        fflush(stdout);
    }
}

static void start_election(raft_node_t* node, int world_size) {
    node->state = STATE_CANDIDATE;
    node->currentTerm += 1;
    node->votedFor = node->id;
    node->votes_received = 1;

    for (int i = 0; i < MAX_NODES + 1; i++) node->vote_seen[i] = 0;
    node->vote_seen[node->id] = 1;

    node->timeout_ms = random_timeout_ms();
    node->last_received_time = MPI_Wtime();

    msg_requestvote_t rv;
    rv.term = node->currentTerm;
    rv.candidate_id = node->id;

    printf("[Node %d] Starting election: term=%ld, votedFor=self, broadcasting RequestVote\n",
           node->id, (long)node->currentTerm);
    fflush(stdout);

    broadcast_request_vote(world_size, &rv);
}

static void leader_send_heartbeats(raft_node_t* node, int world_size) {
    double now = MPI_Wtime();
    if (node->state != STATE_LEADER) return;
    if (!node->enabled) return;
    if (now < node->next_heartbeat_time) return;

    msg_heartbeat_t hb;
    hb.term = node->currentTerm;
    hb.leader_id = node->id;

    for (int p = 0; p < world_size; p++) {
        if (p == node->id) continue;
        MPI_Request r;
        MPI_Isend(&hb, (int)sizeof(hb), MPI_BYTE, p, TAG_HEARTBEAT, MPI_COMM_WORLD, &r);
        MPI_Request_free(&r);
    }

    node->next_heartbeat_time = now + HEARTBEAT_SEC;
}

static void check_timeout_and_maybe_start_election(raft_node_t* node, int world_size) {
    if (!node->enabled) return;
    if (node->state == STATE_LEADER) return;

    double now = MPI_Wtime();
    double timeout_sec = node->timeout_ms / 1000.0;

    if ((now - node->last_received_time) >= timeout_sec) {
        start_election(node, world_size);
    }
}

static void maybe_simulate_crash_or_restart(raft_node_t* node, double elapsed) {
    static double leader_since = -1.0;
    static int crash_count = 0;
    const int MAX_CRASHES = 3;

    if (node->state == STATE_LEADER && node->enabled && leader_since < 0) {
        leader_since = MPI_Wtime();
    }

    if (node->state != STATE_LEADER) {
        leader_since = -1.0;
    }

    if (node->enabled && node->state == STATE_LEADER && leader_since > 0) {
        double time_as_leader = MPI_Wtime() - leader_since;

        if (time_as_leader >= 2.0 && crash_count < MAX_CRASHES) {
            node->enabled = 0;
            crash_count++;
            
            printf("\n[Node %d] !!! CRASHED !!! (Held leadership for %.4f seconds, Term %ld)\n\n", 
                   node->id, time_as_leader, (long)node->currentTerm);
            fflush(stdout);
            
            leader_since = -1.0;
        }
    }

    if (!node->enabled && elapsed >= 8.0) {
        node->enabled = 1;
        node->state = STATE_FOLLOWER;
        node->last_received_time = MPI_Wtime();
        printf("[Node %d] *** RESTARTED ***\n", node->id);
        fflush(stdout);
    }
}

int main(int argc, char** argv) {
    MPI_Init(&argc, &argv);

    int rank, size;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    if (size < 2 || size > MAX_NODES) {
        if (rank == 0) fprintf(stderr, "Run with [2, %d] MPI processes.\n", MAX_NODES);
        MPI_Finalize();
        return 1;
    }

    srand((unsigned)time(NULL) + (unsigned)(rank * MAGIC_NUMBER_SPREAD));

    raft_node_t node;
    node.id = rank;
    node.state = STATE_FOLLOWER;
    node.currentTerm = 0;
    node.votedFor = -1;
    node.votes_received = 0;
    for (int i = 0; i < MAX_NODES + 1; i++) node.vote_seen[i] = 0;

    node.timeout_ms = random_timeout_ms();
    node.last_received_time = MPI_Wtime();
    node.next_heartbeat_time = 0.0;
    node.enabled = 1;

    printf("[Node %d] Boot: FOLLOWER, timeout=%dms, term=%ld\n",
           node.id, node.timeout_ms, (long)node.currentTerm);
    fflush(stdout);

    double start = MPI_Wtime();
    while (MPI_Wtime() - start < 12.0) {
        double elapsed = MPI_Wtime() - start;

        maybe_simulate_crash_or_restart(&node, elapsed);

        int flag = 0;
        MPI_Status st;

        do {
            MPI_Iprobe(MPI_ANY_SOURCE, MPI_ANY_TAG, MPI_COMM_WORLD, &flag, &st);
            if (!flag) break;

            if (st.MPI_TAG == TAG_REQUEST_VOTE) {
                msg_requestvote_t rv;
                MPI_Recv(&rv, (int)sizeof(rv), MPI_BYTE, st.MPI_SOURCE, TAG_REQUEST_VOTE, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

                if (node.enabled) handle_request_vote(&node, &rv);

            } else if (st.MPI_TAG == TAG_REQUEST_VOTE_RESP) {
                msg_requestvote_response_t resp;
                MPI_Recv(&resp, (int)sizeof(resp), MPI_BYTE, st.MPI_SOURCE, TAG_REQUEST_VOTE_RESP, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

                if (node.enabled) handle_request_vote_response(&node, &resp, st.MPI_SOURCE, size);

            } else if (st.MPI_TAG == TAG_HEARTBEAT) {
                msg_heartbeat_t hb;
                MPI_Recv(&hb, (int)sizeof(hb), MPI_BYTE, st.MPI_SOURCE, TAG_HEARTBEAT, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

                if (node.enabled) {
                    handle_heartbeat(&node, &hb);
                    msg_heartbeat_response_t hbr = { .term = node.currentTerm };
                    send_heartbeat_response(st.MPI_SOURCE, &hbr);
                }

            } else if (st.MPI_TAG == TAG_HEARTBEAT_RESP) {
                msg_heartbeat_response_t hbr;
                MPI_Recv(&hbr, (int)sizeof(hbr), MPI_BYTE, st.MPI_SOURCE, TAG_HEARTBEAT_RESP, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

                if (node.enabled && node.state == STATE_LEADER) {
                    handle_heartbeat_response(&node, &hbr);
                }

            } else {
                char junk[256];
                int count = 0;
                MPI_Get_count(&st, MPI_BYTE, &count);
                if (count > (int)sizeof(junk)) count = (int)sizeof(junk);
                MPI_Recv(junk, count, MPI_BYTE, st.MPI_SOURCE, st.MPI_TAG, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
            }
        } while (flag);

        leader_send_heartbeats(&node, size);
        check_timeout_and_maybe_start_election(&node, size);

        usleep(1000);
    }

    MPI_Finalize();
    return 0;
}
