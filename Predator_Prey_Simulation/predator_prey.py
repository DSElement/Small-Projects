import pygame
import random
import math

try:
    import matplotlib.pyplot as plt
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False

# Screen dimensions
WIDTH, HEIGHT = 800, 600

# Colors
BACKGROUND_COLOR = (46, 40, 54)
PREY_COLOR = (42, 248, 31)
PREDATOR_COLOR = (203, 28, 28)
BERRY_COLOR = (200, 50, 200)
JACKFRUIT_COLOR = (255, 200, 0)
CORPSE_COLOR = (130, 130, 130)
ROCK_COLOR = (80, 80, 110)
TEXT_COLOR = (220, 220, 220)

# Simulation parameters
MAX_FOOD = 300

LIVE_PREY_ENERGY_GAIN = 30
CORPSE_ENERGY_GAIN = LIVE_PREY_ENERGY_GAIN // 2

# Energies
PREY_MAX_ENERGY = 100
PREY_INIT_ENERGY = 60
PRED_MAX_ENERGY = 150
PRED_INIT_ENERGY = 120

# Speeds 
PREY_SPEED_HIGH = 15.0
PREY_SPEED_LOW = 17.0
PRED_SPEED_HIGH = 14.0
PRED_SPEED_LOW = 21.0
PREY_SPRINT_SPEED = 22.0
ENERGY_SPEED_THRESHOLD_FRACTION = 0.6  # 60%

# Foraging
PREY_FORAGE_THRESHOLD_FRACTION = 0.65  # 65%
PREDATOR_VISION_RADIUS = 100.0
PRED_SATIATED_FRACTION = 0.8

# Sprint
SPRINT_DURATION = 6.0
SPRINT_COOLDOWN = 20.0
SPRINT_TRIGGER_DIST = 15.0
SPRINT_COST_MULTIPLIER = 2.0

# Flocking (moderate, escapable)
FLOCK_RADIUS = 50.0
FLOCK_NEIGHBORS_MIN = 3
FLOCK_SPEED_BONUS = 1.1
FLOCK_ALIGNMENT_WEIGHT = 0.4
FLOCK_COHESION_WEIGHT = 0.25
FLOCK_SEPARATION_WEIGHT = 0.7
FLOCK_STRENGTH = 0.6        # cap flock influence
FLOCK_IGNORE_PROB = 0.1     # chance to ignore flocking this frame
SEPARATION_RADIUS = 20.0
WANDER_STRENGTH = 0.1       # random drift to allow leaving flocks

# Obstacles
ROCK_COUNT = 7
OBSTACLE_AVOID_RADIUS = 60.0
OBSTACLE_AVOID_STRENGTH = 1.5

FOOD_ROCK_BUFFER = 50.0  # distance from rock surface

# Reproduction
REPRO_ENERGY_THRESHOLD_FRACTION_PREY = 0.60
REPRO_ENERGY_THRESHOLD_FRACTION_PRED = 0.80
REPRO_COST_FRACTION = 0.2  # 20% of max energy

PREY_REPRO_COOLDOWN = 18.0 
PRED_REPRO_COOLDOWN = 40.0
NEWBORN_COOLDOWN_MULT = 1.0
MATING_STOP_TIME = 3.0
MATE_SEARCH_RADIUS = 150.0

# Corpses
CORPSE_LIFETIME = 5.0
CORPSE_RADIUS = 4

# Controls limits
FRUIT_SPAWN_MULT_MIN = 0.5
FRUIT_SPAWN_MULT_MAX = 25.0
ENERGY_RATE_MULT_MIN = 0.25
ENERGY_RATE_MULT_MAX = 20.0
TIME_SCALE_MIN = 0.25
TIME_SCALE_MAX = 25.0

pygame.init()
screen = pygame.display.set_mode((WIDTH, HEIGHT))
pygame.display.set_caption("Predator-Prey Simulation")
clock = pygame.time.Clock()
font_small = pygame.font.SysFont(None, 18)
font_default = pygame.font.SysFont(None, 24)


def clamp(value, min_value, max_value):
    return max(min_value, min(max_value, value))


def draw_energy_bar(surface, position, fraction, width=20, height=3,
                    back_color=(40, 40, 40), fill_color=(0, 255, 0)):
    """Draw a small energy bar above an entity."""
    fraction = clamp(fraction, 0.0, 1.0)
    x = int(position.x)
    y = int(position.y) - 10  # slightly above the entity
    bar_x = x - width // 2
    bar_y = y
    bg_rect = pygame.Rect(bar_x, bar_y, width, height)
    fg_rect = pygame.Rect(bar_x, bar_y, int(width * fraction), height)
    pygame.draw.rect(surface, back_color, bg_rect)
    if fraction > 0:
        pygame.draw.rect(surface, fill_color, fg_rect)


class Fruit:
    def __init__(self, obstacles):
        # Ensure we don't spawn on top of rocks OR too close to them
        while True:
            pos = pygame.math.Vector2(
                random.uniform(0, WIDTH),
                random.uniform(0, HEIGHT)
            )

            too_close = False
            for obs in obstacles:
                dist, _ = obs.distance_and_direction_to_point(pos)
                # dist is distance from obstacle surface (negative if inside),
                # so require at least FOOD_ROCK_BUFFER away from the surface
                if dist < FOOD_ROCK_BUFFER:
                    too_close = True
                    break

            if not too_close:
                self.position = pos
                break

    def update(self):
        pass  # fruits don't move


class Berry(Fruit):
    ENERGY = 10
    SPAWN_CHANCE = 0.3   # base common
    COLOR = BERRY_COLOR
    RADIUS = 3

    def draw(self, surface):
        pygame.draw.circle(surface, self.COLOR,
                           (int(self.position.x), int(self.position.y)),
                           self.RADIUS)


class Jackfruit(Fruit):
    ENERGY = 15
    SPAWN_CHANCE = 0.1   # base rare
    COLOR = JACKFRUIT_COLOR
    SIZE = 6  # square size

    def draw(self, surface):
        x, y = int(self.position.x), int(self.position.y)
        pygame.draw.rect(surface, self.COLOR,
                         pygame.Rect(x - self.SIZE // 2, y - self.SIZE // 2,
                                     self.SIZE, self.SIZE))


class Rock:
    # Some are circles, some rectangles
    def __init__(self, shape_type, pos, size1, size2=None):
        self.shape_type = shape_type  # "circle" or "rect"
        self.pos = pygame.math.Vector2(pos)
        if shape_type == "circle":
            self.radius = size1
        else:
            self.width = size1
            self.height = size2 if size2 is not None else size1

    def draw(self, surface):
        if self.shape_type == "circle":
            pygame.draw.circle(surface, ROCK_COLOR,
                               (int(self.pos.x), int(self.pos.y)), int(self.radius))
        else:
            rect = pygame.Rect(
                int(self.pos.x - self.width / 2),
                int(self.pos.y - self.height / 2),
                int(self.width),
                int(self.height),
            )
            pygame.draw.rect(surface, ROCK_COLOR, rect)

    def contains_point(self, point):
        if self.shape_type == "circle":
            return self.pos.distance_to(point) <= self.radius
        # rect
        return (abs(point.x - self.pos.x) <= self.width / 2 and
                abs(point.y - self.pos.y) <= self.height / 2)

    def distance_and_direction_to_point(self, point):
        """Return (distance_from_surface, direction_vector_from_obstacle_to_point)."""
        if self.shape_type == "circle":
            direction = point - self.pos
            dist_center = direction.length()
            if dist_center > 0:
                return dist_center - self.radius, direction.normalize()
            else:
                return 0.0, pygame.math.Vector2(0, 0)
        # rect: closest point on rect to the given point
        closest_x = clamp(point.x, self.pos.x - self.width / 2, self.pos.x + self.width / 2)
        closest_y = clamp(point.y, self.pos.y - self.height / 2, self.pos.y + self.height / 2)
        closest = pygame.math.Vector2(closest_x, closest_y)
        direction = point - closest
        dist = direction.length()
        if dist > 0:
            return dist, direction.normalize()
        else:
            # Inside the rect; push upwards arbitrarily
            return 0.0, pygame.math.Vector2(0, -1)


class Corpse:
    def __init__(self, position):
        self.position = pygame.math.Vector2(position)
        self.age = 0.0  # seconds

    def update(self, dt):
        self.age += dt

    def is_expired(self):
        return self.age >= CORPSE_LIFETIME

    def draw(self, surface):
        pygame.draw.circle(surface, CORPSE_COLOR,
                           (int(self.position.x), int(self.position.y)),
                           CORPSE_RADIUS)


class Prey:
    def __init__(self, position=None, newborn=False):
        if position is None:
            self.position = pygame.math.Vector2(random.uniform(0, WIDTH), random.uniform(0, HEIGHT))
        else:
            self.position = pygame.math.Vector2(position)
        angle = random.uniform(0, 2 * math.pi)
        self.velocity = pygame.math.Vector2(math.cos(angle), math.sin(angle))
        self.max_energy = PREY_MAX_ENERGY
        self.energy = PREY_INIT_ENERGY
        self.move_cost = 0.05
        self.vision_radius = 80.0

        self.is_sprinting = False
        self.sprint_time_left = 0.0
        self.sprint_cooldown_left = 0.0

        # Reproduction
        self.repro_cooldown_total = PREY_REPRO_COOLDOWN * (NEWBORN_COOLDOWN_MULT if newborn else 1.0)
        self.repro_cooldown_left = random.uniform(self.repro_cooldown_total * 0.5, self.repro_cooldown_total)
        """
        if not newborn:
            self.repro_cooldown_left = 0.0
        """
        self.is_mating = False
        self.mating_partner = None
        self.mating_timer = 0.0

    def current_speed(self, flock_neighbors):
        # Base on energy
        threshold = ENERGY_SPEED_THRESHOLD_FRACTION * self.max_energy
        if self.energy >= threshold:
            speed = PREY_SPEED_HIGH
        else:
            speed = PREY_SPEED_LOW

        # Flocking speed bonus
        if flock_neighbors >= FLOCK_NEIGHBORS_MIN:
            speed *= FLOCK_SPEED_BONUS

        # Sprint overrides
        if self.is_sprinting:
            speed = PREY_SPRINT_SPEED

        return speed

    def can_reproduce(self):
        if self.is_sprinting:
            return False
        if self.is_mating:
            return False
        if self.repro_cooldown_left > 0.0:
            return False
        if self.energy < REPRO_ENERGY_THRESHOLD_FRACTION_PREY * self.max_energy:
            return False
        return True

    def start_mating(self, partner):
        self.is_mating = True
        self.mating_partner = partner
        self.mating_timer = MATING_STOP_TIME

    def finish_mating(self):
        self.is_mating = False
        self.mating_partner = None
        self.mating_timer = 0.0
        self.repro_cooldown_total = PREY_REPRO_COOLDOWN
        self.repro_cooldown_left = random.uniform(self.repro_cooldown_total * 0.5, self.repro_cooldown_total)
        # Apply reproduction energy cost
        self.energy -= REPRO_COST_FRACTION * self.max_energy
        self.energy = max(self.energy, 0.0)

    def update(self, predators, fruits, all_prey, rocks, dt, energy_rate_multiplier):
        if self.energy <= 0:
            return

        # Timers
        if self.sprint_cooldown_left > 0:
            self.sprint_cooldown_left = max(0.0, self.sprint_cooldown_left - dt)
        if self.is_sprinting:
            self.sprint_time_left -= dt
            if self.sprint_time_left <= 0:
                self.is_sprinting = False
        if self.repro_cooldown_left > 0.0:
            self.repro_cooldown_left = max(0.0, self.repro_cooldown_left - dt)

        if self.is_mating:
            # Stay still while mating
            self.velocity = pygame.math.Vector2(0, 0)
            self.mating_timer -= dt
            return

        # Decide behavior
        desired = pygame.math.Vector2(self.velocity)

        # 1. Flee from nearest predator within vision radius
        nearest_pred = None
        nearest_distance = self.vision_radius
        for pred in predators:
            d = self.position.distance_to(pred.position)
            if d < nearest_distance:
                nearest_distance = d
                nearest_pred = pred

        fleeing = False
        if nearest_pred is not None:
            # Flee direction
            flee_direction = (self.position - nearest_pred.position)
            if flee_direction.length_squared() > 0:
                flee_direction = flee_direction.normalize()
            desired = flee_direction
            fleeing = True

            # Sprint trigger if extremely close
            if nearest_distance < SPRINT_TRIGGER_DIST and not self.is_sprinting and self.sprint_cooldown_left <= 0:
                self.is_sprinting = True
                self.sprint_time_left = SPRINT_DURATION
                self.sprint_cooldown_left = SPRINT_COOLDOWN

        # 2. If not fleeing & low energy, seek nearest fruit
        forage_threshold = PREY_FORAGE_THRESHOLD_FRACTION * self.max_energy
        hungry = self.energy < forage_threshold

        if not fleeing and hungry and fruits:
            nearest_fruit = min(fruits, key=lambda f: self.position.distance_to(f.position))
            direction = (nearest_fruit.position - self.position)
            if direction.length_squared() > 0:
                desired = direction.normalize()

        # 3. Flocking (alignment / cohesion / separation)
        neighbors = []
        for other in all_prey:
            if other is self:
                continue
            d = self.position.distance_to(other.position)
            if d < FLOCK_RADIUS:
                neighbors.append((other, d))

        flock_neighbors_count = len(neighbors)
        if neighbors and not fleeing and not hungry:  # <<< key change: no flocking when hungry
            alignment = pygame.math.Vector2(0, 0)
            cohesion = pygame.math.Vector2(0, 0)
            separation = pygame.math.Vector2(0, 0)
            for other, dist in neighbors:
                alignment += other.velocity
                cohesion += other.position
                if dist < SEPARATION_RADIUS:
                    if dist > 0:
                        separation += (self.position - other.position) / dist
            # alignment
            if alignment.length_squared() > 0:
                alignment = alignment.normalize()
            # cohesion
            cohesion = (cohesion / len(neighbors) - self.position)
            if cohesion.length_squared() > 0:
                cohesion = cohesion.normalize()
            # separation
            if separation.length_squared() > 0:
                separation = separation.normalize()

            flock_vector = (alignment * FLOCK_ALIGNMENT_WEIGHT +
                            cohesion * FLOCK_COHESION_WEIGHT +
                            separation * FLOCK_SEPARATION_WEIGHT)

            # Apply flocking moderately, and only sometimes
            if flock_vector.length_squared() > 0:
                if random.random() > FLOCK_IGNORE_PROB:
                    flock_vector = flock_vector.normalize() * FLOCK_STRENGTH
                    desired += flock_vector

        # 4. Obstacle avoidance (steering)
        avoid = pygame.math.Vector2(0, 0)
        for rock in rocks:
            dist, dir_vec = rock.distance_and_direction_to_point(self.position)
            if dist < OBSTACLE_AVOID_RADIUS:
                if dist > 0:
                    strength = (OBSTACLE_AVOID_RADIUS - dist) / OBSTACLE_AVOID_RADIUS
                    avoid += dir_vec * strength * OBSTACLE_AVOID_STRENGTH
        if avoid.length_squared() > 0:
            desired += avoid

        # 5. Random wander to allow drifting out of flocks (only if not fleeing)
        if not fleeing and not hungry:  # when hungry, don't random-walk away from food
            wander = pygame.math.Vector2(random.uniform(-1, 1), random.uniform(-1, 1))
            if wander.length_squared() > 0:
                wander = wander.normalize() * WANDER_STRENGTH
                desired += wander

        if desired.length_squared() > 0:
            desired = desired.normalize()
        self.velocity = desired

        # Move
        speed = self.current_speed(flock_neighbors_count)
        self.position += self.velocity * speed * dt

        # Rock collision & bounce
        for rock in rocks:
            if rock.contains_point(self.position):
                dist, normal = rock.distance_and_direction_to_point(self.position)
                if normal.length_squared() > 0:
                    # Push out a bit
                    push = 1.0
                    if dist < 0:
                        push = -dist + 1.0
                    self.position += normal * push
                    # Reflect velocity
                    v = self.velocity
                    dot = v.x * normal.x + v.y * normal.y
                    self.velocity = v - 2 * dot * normal

        # Bounce off edges (simple clamp)
        if self.position.x < 0 or self.position.x > WIDTH:
            self.position.x = clamp(self.position.x, 0, WIDTH)
            self.velocity.x *= -1
        if self.position.y < 0 or self.position.y > HEIGHT:
            self.position.y = clamp(self.position.y, 0, HEIGHT)
            self.velocity.y *= -1

        # Energy cost
        cost = self.move_cost
        if self.is_sprinting:
            cost *= SPRINT_COST_MULTIPLIER
        self.energy = max(0.0, self.energy - cost * energy_rate_multiplier * dt)


    def eat(self, fruit):
        self.energy = min(self.max_energy, self.energy + fruit.ENERGY)

    def draw(self, surface):
        pygame.draw.circle(surface, PREY_COLOR,
                           (int(self.position.x), int(self.position.y)), 4)
        draw_energy_bar(surface, self.position,
                        self.energy / self.max_energy if self.max_energy > 0 else 0.0,
                        fill_color=(0, 255, 0))


class Predator:
    def __init__(self, position=None, newborn=False):
        if position is None:
            self.position = pygame.math.Vector2(random.uniform(0, WIDTH), random.uniform(0, HEIGHT))
        else:
            self.position = pygame.math.Vector2(position)
        angle = random.uniform(0, 2 * math.pi)
        self.velocity = pygame.math.Vector2(math.cos(angle), math.sin(angle))
        self.max_energy = PRED_MAX_ENERGY
        self.energy = PRED_INIT_ENERGY
        self.move_cost = 0.5

        # Reproduction
        self.repro_cooldown_total = PRED_REPRO_COOLDOWN * (NEWBORN_COOLDOWN_MULT if newborn else 1.0)
        self.repro_cooldown_left = random.uniform(self.repro_cooldown_total * 0.5, self.repro_cooldown_total)
        
        """
        if not newborn:
            self.repro_cooldown_left = 0.0
        """
        self.is_mating = False
        self.mating_partner = None
        self.mating_timer = 0.0

    def current_speed(self):
        threshold = ENERGY_SPEED_THRESHOLD_FRACTION * self.max_energy
        if self.energy >= threshold:
            speed = PRED_SPEED_HIGH
        else:
            speed = PRED_SPEED_LOW
        return speed

    def can_reproduce(self):
        if self.is_mating:
            return False
        if self.repro_cooldown_left > 0.0:
            return False
        if self.energy < REPRO_ENERGY_THRESHOLD_FRACTION_PRED * self.max_energy:
            return False
        return True

    def start_mating(self, partner):
        self.is_mating = True
        self.mating_partner = partner
        self.mating_timer = MATING_STOP_TIME

    def finish_mating(self):
        self.is_mating = False
        self.mating_partner = None
        self.mating_timer = 0.0
        self.repro_cooldown_total = PRED_REPRO_COOLDOWN
        self.repro_cooldown_left = random.uniform(self.repro_cooldown_total * 0.5, self.repro_cooldown_total)
        # Apply reproduction energy cost
        self.energy -= REPRO_COST_FRACTION * self.max_energy
        self.energy = max(self.energy, 0.0)

    def is_satiated(self):
        return self.energy > PRED_SATIATED_FRACTION * self.max_energy

    def update(self, prey_list, rocks, dt, energy_rate_multiplier):
        if self.energy <= 0:
            return

        if self.repro_cooldown_left > 0.0:
            self.repro_cooldown_left = max(0.0, self.repro_cooldown_left - dt)

        if self.is_mating:
            self.velocity = pygame.math.Vector2(0, 0)
            self.mating_timer -= dt
            return

        # Simple hunting behavior: move towards the nearest prey
        visible_prey = [
            prey for prey in prey_list
            if self.position.distance_to(prey.position) < PREDATOR_VISION_RADIUS
        ]

        desired = pygame.math.Vector2(self.velocity)

        if not self.is_satiated() and visible_prey:
            nearest_prey = min(visible_prey, key=lambda prey: self.position.distance_to(prey.position))
            direction = (nearest_prey.position - self.position)
            if direction.length_squared() > 0:
                desired = direction.normalize()
        else:
            # satiated or no prey in sight → just keep going / wander
            desired = pygame.math.Vector2(self.velocity)

        # Obstacle avoidance (steering)
        avoid = pygame.math.Vector2(0, 0)
        for rock in rocks:
            dist, dir_vec = rock.distance_and_direction_to_point(self.position)
            if dist < OBSTACLE_AVOID_RADIUS:
                if dist > 0:
                    strength = (OBSTACLE_AVOID_RADIUS - dist) / OBSTACLE_AVOID_RADIUS
                    avoid += dir_vec * strength * OBSTACLE_AVOID_STRENGTH
        if avoid.length_squared() > 0:
            desired += avoid

        if desired.length_squared() > 0:
            desired = desired.normalize()
        self.velocity = desired

        speed = self.current_speed()
        self.position += self.velocity * speed * dt

        # Rock collision & bounce
        for rock in rocks:
            if rock.contains_point(self.position):
                dist, normal = rock.distance_and_direction_to_point(self.position)
                if normal.length_squared() > 0:
                    push = 1.0
                    if dist < 0:
                        push = -dist + 1.0
                    self.position += normal * push
                    v = self.velocity
                    dot = v.x * normal.x + v.y * normal.y
                    self.velocity = v - 2 * dot * normal

        # Bounce off the edges
        if self.position.x < 0 or self.position.x > WIDTH:
            self.position.x = clamp(self.position.x, 0, WIDTH)
            self.velocity.x *= -1
        if self.position.y < 0 or self.position.y > HEIGHT:
            self.position.y = clamp(self.position.y, 0, HEIGHT)
            self.velocity.y *= -1

        # Energy cost
        self.energy = max(0.0, self.energy - self.move_cost * energy_rate_multiplier * dt)

    def eat_prey(self):
        self.energy = min(self.max_energy, self.energy + LIVE_PREY_ENERGY_GAIN)

    def eat_corpse(self):
        self.energy = min(self.max_energy, self.energy + CORPSE_ENERGY_GAIN)

    def draw(self, surface):
        # Calculate the angle in degrees between the velocity and the x-axis
        if self.velocity.length_squared() == 0:
            angle = 0
        else:
            angle = self.velocity.angle_to(pygame.math.Vector2(1, 0))

        # Define the triangle points relative to the origin, pointing right
        point_list = [
            pygame.math.Vector2(10, 0),    # Tip of the triangle
            pygame.math.Vector2(-5, -5),   # Bottom left
            pygame.math.Vector2(-5, 5),    # Bottom right
        ]

        # Rotate the points and translate to the predator's position
        rotated_point_list = [self.position + p.rotate(-angle) for p in point_list]

        # Draw the predator as a triangle
        pygame.draw.polygon(surface, PREDATOR_COLOR, rotated_point_list)
        draw_energy_bar(surface, self.position,
                        self.energy / self.max_energy if self.max_energy > 0 else 0.0,
                        fill_color=(255, 80, 80))


def create_rocks():
    rocks = []
    for _ in range(ROCK_COUNT):
        shape_type = random.choice(["circle", "rect"])
        x = random.uniform(100, WIDTH - 100)
        y = random.uniform(100, HEIGHT - 100)
        if shape_type == "circle":
            radius = random.uniform(20, 50)
            rocks.append(Rock("circle", (x, y), radius))
        else:
            w = random.uniform(40, 80)
            h = random.uniform(30, 70)
            rocks.append(Rock("rect", (x, y), w, h))
    return rocks


def spawn_initial_populations():
    prey_list = []
    predator_list = []
    # For simplicity, we don't avoid rocks when spawning agents; they'll steer away
    for _ in range(50):
        prey_list.append(Prey(newborn=False))
    for _ in range(5):
        predator_list.append(Predator(newborn=False))
    return prey_list, predator_list


def spawn_initial_fruits(rocks):
    fruits = []
    for _ in range(10):
        fruits.append(Berry(rocks))
    for _ in range(4):
        fruits.append(Jackfruit(rocks))
    return fruits


def draw_legend(fruit_spawn_mult, energy_rate_mult, time_scale):
    prey_text = font_default.render("Prey", True, PREY_COLOR)
    predator_text = font_default.render("Predator", True, PREDATOR_COLOR)
    berry_text = font_default.render("Berry", True, BERRY_COLOR)
    jackfruit_text = font_default.render("Jackfruit", True, JACKFRUIT_COLOR)
    controls1 = font_small.render("Q/W: Fruit spawn x{:.2f}".format(fruit_spawn_mult), True, TEXT_COLOR)
    controls2 = font_small.render("A/S: Energy rate x{:.2f}".format(energy_rate_mult), True, TEXT_COLOR)
    controls3 = font_small.render("Z/X: Time scale x{:.2f}".format(time_scale), True, TEXT_COLOR)

    screen.blit(prey_text, (10, 10))
    screen.blit(predator_text, (10, 30))
    screen.blit(berry_text, (10, 50))
    screen.blit(jackfruit_text, (10, 70))
    screen.blit(controls1, (10, 100))
    screen.blit(controls2, (10, 120))
    screen.blit(controls3, (10, 140))


def draw_stats(prey_list, predator_list):
    prey_count_text = font_default.render(f"Prey Count: {len(prey_list)}", True, TEXT_COLOR)
    predator_count_text = font_default.render(f"Predator Count: {len(predator_list)}", True, TEXT_COLOR)
    screen.blit(prey_count_text, (10, HEIGHT - 40))
    screen.blit(predator_count_text, (10, HEIGHT - 20))


def draw_mini_graph(data_list, rect, color):
    if len(data_list) < 2:
        return
    sub = data_list[-80:]  # last 80 samples
    max_val = max(sub)
    if max_val <= 0:
        return
    if len(sub) == 1:
        return
    step_x = rect.width / (len(sub) - 1)
    points = []
    for i, v in enumerate(sub):
        x = rect.left + i * step_x
        y = rect.bottom - (v / max_val) * rect.height
        points.append((x, y))
    if len(points) >= 2:
        pygame.draw.lines(screen, color, False, points, 1)


def save_graphs(time_hist, prey_hist, pred_hist, prey_birth_hist, pred_birth_hist):
    if not HAS_MATPLOTLIB or len(time_hist) < 2:
        return
    # Population graph
    plt.figure()
    plt.plot(time_hist, prey_hist, label="Prey")
    plt.plot(time_hist, pred_hist, label="Predators")
    plt.xlabel("Time (s)")
    plt.ylabel("Population")
    plt.legend()
    plt.tight_layout()
    plt.savefig("population_history.png")
    plt.close()

    # Birth rates graph
    plt.figure()
    plt.plot(time_hist, prey_birth_hist, label="Prey births")
    plt.plot(time_hist, pred_birth_hist, label="Predator births")
    plt.xlabel("Time (s)")
    plt.ylabel("Births per interval")
    plt.legend()
    plt.tight_layout()
    plt.savefig("birth_rates_history.png")
    plt.close()


def find_mate(agent, agents):
    if not agent.can_reproduce():
        return None
    best = None
    best_dist = MATE_SEARCH_RADIUS
    for other in agents:
        if other is agent:
            continue
        if not other.can_reproduce():
            continue
        d = agent.position.distance_to(other.position)
        if d < best_dist:
            best_dist = d
            best = other
    return best


def adjust_position_off_rocks(pos, rocks):
    position = pygame.math.Vector2(pos)
    max_iters = 10
    for _ in range(max_iters):
        if not any(r.contains_point(position) for r in rocks):
            return position
        # push away from first rock found
        rock = next(r for r in rocks if r.contains_point(position))
        dist, dir_vec = rock.distance_and_direction_to_point(position)
        if dir_vec.length_squared() == 0:
            dir_vec = pygame.math.Vector2(random.uniform(-1, 1), random.uniform(-1, 1))
            if dir_vec.length_squared() > 0:
                dir_vec = dir_vec.normalize()
        position += dir_vec * 10
    return position


def handle_reproduction(agent_list, dt, rocks):
    births = 0
    
    # Finish matings and spawn offspring (only from one of the pair)
    new_agents = []
    for agent in agent_list:
        if agent.is_mating and agent.mating_timer <= 0 and agent.mating_partner is not None:
            partner = agent.mating_partner
            if partner in agent_list and partner.mating_partner is agent:
                # Spawn child only once per pair
                if id(agent) < id(partner):
                    # Child position: between parents
                    child_pos = (agent.position + partner.position) / 2
                    child_pos = adjust_position_off_rocks(child_pos, rocks)
                    if isinstance(agent, Prey):
                        child = Prey(position=child_pos, newborn=True)
                    else:
                        child = Predator(position=child_pos, newborn=True)
                    new_agents.append(child)
                    births += 1
                # finish mating for both
                agent.finish_mating()
                partner.finish_mating()
    agent_list.extend(new_agents)

    # Start new matings
    for agent in agent_list:
        if agent.can_reproduce():
            mate = find_mate(agent, agent_list)
            if mate is not None and not mate.is_mating:
                agent.start_mating(mate)
                mate.start_mating(agent)
    return births


def main():
    rocks = create_rocks()
    prey_list, predator_list = spawn_initial_populations()
    fruit_list = spawn_initial_fruits(rocks)
    corpse_list = []

    # Controls
    fruit_spawn_mult = 10.0
    energy_rate_mult = 3.0
    time_scale = 16.0  # faster default simulation

    running = True
    sim_time = 0.0

    # History tracking
    time_history = []
    prey_history = []
    predator_history = []
    prey_birth_history = []
    pred_birth_history = []
    history_interval = 0.5
    time_since_last_sample = 0.0
    prey_births_accum = 0
    pred_births_accum = 0

    while running:
        dt_ms = clock.tick(60)
        dt = (dt_ms / 1000.0) * time_scale
        sim_time += dt
        time_since_last_sample += dt

        # Event handling
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_q:
                    fruit_spawn_mult = clamp(fruit_spawn_mult - 0.2,
                                             FRUIT_SPAWN_MULT_MIN, FRUIT_SPAWN_MULT_MAX)
                elif event.key == pygame.K_w:
                    fruit_spawn_mult = clamp(fruit_spawn_mult + 0.2,
                                             FRUIT_SPAWN_MULT_MIN, FRUIT_SPAWN_MULT_MAX)
                elif event.key == pygame.K_a:
                    energy_rate_mult = clamp(energy_rate_mult - 0.2,
                                             ENERGY_RATE_MULT_MIN, ENERGY_RATE_MULT_MAX)
                elif event.key == pygame.K_s:
                    energy_rate_mult = clamp(energy_rate_mult + 0.2,
                                             ENERGY_RATE_MULT_MIN, ENERGY_RATE_MULT_MAX)
                elif event.key == pygame.K_z:
                    time_scale = clamp(time_scale - 0.2,
                                       TIME_SCALE_MIN, TIME_SCALE_MAX)
                elif event.key == pygame.K_x:
                    time_scale = clamp(time_scale + 0.2,
                                       TIME_SCALE_MIN, TIME_SCALE_MAX)

        screen.fill(BACKGROUND_COLOR)

        # Spawn fruits
        if len(fruit_list) < MAX_FOOD:
            if random.random() < Berry.SPAWN_CHANCE * fruit_spawn_mult * dt:
                fruit_list.append(Berry(rocks))
            if random.random() < Jackfruit.SPAWN_CHANCE * fruit_spawn_mult * dt:
                fruit_list.append(Jackfruit(rocks))

        # Update corpses
        for corpse in corpse_list:
            corpse.update(dt)
        corpse_list = [c for c in corpse_list if not c.is_expired()]

        # Update prey
        for prey in prey_list:
            prey.update(predator_list, fruit_list, prey_list, rocks, dt, energy_rate_mult)

        # Update predators
        for predator in predator_list:
            predator.update(prey_list, rocks, dt, energy_rate_mult)

        # Handle deaths from energy (starvation)
        # Prey: spawn corpse on starvation, adjusted off rocks
        alive_prey = []
        for prey in prey_list:
            if prey.energy <= 0:
                pos = adjust_position_off_rocks(prey.position, rocks)
                corpse_list.append(Corpse(pos))
            else:
                alive_prey.append(prey)
        prey_list = alive_prey

        # Predators: die silently
        predator_list = [p for p in predator_list if p.energy > 0]

        # Predator eats prey (no corpse)
        for predator in predator_list:
            for prey in prey_list[:]:
                if predator.position.distance_to(prey.position) < 6:
                    prey_list.remove(prey)
                    predator.eat_prey()
                    break  # one prey per predator per update

        # Predator eats corpses
        for predator in predator_list:
            for corpse in corpse_list[:]:
                if predator.position.distance_to(corpse.position) < 6:
                    corpse_list.remove(corpse)
                    predator.eat_corpse()

        # Prey eat fruits (no energy threshold restriction)
        for prey in prey_list:
            for fruit in fruit_list[:]:
                if prey.position.distance_to(fruit.position) < 6:
                    prey.eat(fruit)
                    fruit_list.remove(fruit)

        # Reproduction
        prey_births = handle_reproduction(prey_list, dt, rocks)
        pred_births = handle_reproduction(predator_list, dt, rocks)
        prey_births_accum += prey_births
        pred_births_accum += pred_births

        # History tracking
        if time_since_last_sample >= history_interval:
            time_history.append(sim_time)
            prey_history.append(len(prey_list))
            predator_history.append(len(predator_list))
            prey_birth_history.append(prey_births_accum)
            pred_birth_history.append(pred_births_accum)
            time_since_last_sample = 0.0
            prey_births_accum = 0
            pred_births_accum = 0

        # Draw everything
        # Rocks
        for rock in rocks:
            rock.draw(screen)

        # Fruits
        for fruit in fruit_list:
            fruit.draw(screen)

        # Corpses
        for corpse in corpse_list:
            corpse.draw(screen)

        # Prey and predators
        for prey in prey_list:
            prey.draw(screen)
        for predator in predator_list:
            predator.draw(screen)

        # UI
        draw_legend(fruit_spawn_mult, energy_rate_mult, time_scale)
        draw_stats(prey_list, predator_list)

        # Mini graphs
        graph_width = 200
        graph_height = 60
        pop_rect = pygame.Rect(WIDTH - graph_width - 10, 10, graph_width, graph_height)
        birth_rect = pygame.Rect(WIDTH - graph_width - 10, 80, graph_width, graph_height)
        pygame.draw.rect(screen, (20, 20, 30), pop_rect, 1)
        pygame.draw.rect(screen, (20, 20, 30), birth_rect, 1)
        draw_mini_graph(prey_history, pop_rect, PREY_COLOR)
        draw_mini_graph(predator_history, pop_rect, PREDATOR_COLOR)
        draw_mini_graph(prey_birth_history, birth_rect, PREY_COLOR)
        draw_mini_graph(pred_birth_history, birth_rect, PREDATOR_COLOR)

        pygame.display.flip()

    pygame.quit()
    save_graphs(time_history, prey_history, predator_history,
                prey_birth_history, pred_birth_history)


if __name__ == "__main__":
    main()
