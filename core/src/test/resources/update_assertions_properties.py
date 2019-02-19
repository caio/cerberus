#!/usr/bin/env python

import sys
import json

from collections import defaultdict

input_filename = "sample_recipes.jsonlines"

if len(sys.argv) > 1:
    input_filename = sys.argv[1]

property_to_test = {
    'index_size': lambda r: True,
    'up_to_three_ingredients': lambda r: len(r["ingredients"]) <= 3,
    'five_ingredients': lambda r: len(r["ingredients"]) == 5,
    'total_time_10_15': lambda r: 10 <= r.get("totalTime", 0) <= 25,
}
property_counts = defaultdict(int)


with open(input_filename) as fh:
    for line in fh:
        recipe = json.loads(line.strip())
        for prop, test in property_to_test.items():
            if test(recipe):
                property_counts[prop] += 1

keys = sorted(property_to_test.keys())
for k in keys:
    print("test.{}={}".format(k, property_counts[k]))
