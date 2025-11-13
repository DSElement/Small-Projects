import itertools
import random
import numpy as np

"""
{
        'name': 'after_midnight',
        'start': 0,       
        'end': 6*60,    
        'prob_matrix': {
            ('UPT', 'Center'): 0.2,
            ('UPT', 'Cathedral'): 0.2,
            ('UPT', 'Dorms'): 0.2,
            ('UPT', 'P700'): 0.2,
            ('Center', 'UPT'): 0.2,
            ('Center', 'Cathedral'): 0.2,
            ('Center', 'Dorms'): 0.2,
            ('Center', 'P700'): 0.2,
            ('Cathedral', 'UPT'): 0.2,
            ('Cathedral', 'Center'): 0.2,
            ('Cathedral', 'Dorms'): 0.2,
            ('Cathedral', 'P700'): 0.2,
            ('Dorms', 'UPT'): 0.2,
            ('Dorms', 'Center'): 0.2,
            ('Dorms', 'Cathedral'): 0.2,
            ('Dorms', 'P700'): 0.2,
            ('P700', 'UPT'): 0.2,
            ('P700', 'Center'): 0.2,
            ('P700', 'Cathedral'): 0.2,
            ('P700', 'Dorms'): 0.2,            
        }
    },
"""

upt_station = "upt_station"
center_station = "center_station"
cathedral_station = "cathedral_station"
dorms_station = "dorms_station"
p700_station = "p700_station"

all_stations = [upt_station, center_station, cathedral_station, dorms_station, p700_station]

perms = itertools.permutations(all_stations, 2)  # all 2-item permutations

for p in perms:
    src, dst = p
    val = round(0.05 + np.random.beta(a=3, b=5) * (0.3 - 0.05), 2)
    print(f"    ({src}, {dst}): {val},")

