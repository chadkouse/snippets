#!/usr/bin/env python
"""

Copyright (c) 2004  Dustin Sallings <dustin@spy.net>
"""

import sys
import math

def sum(a):
    return reduce(lambda x,y: x+y, a)

def avg(a):
    return (sum(a) / len(a))

def stddev(a):
    av=float(avg(a))
    return math.sqrt(reduce(lambda c,i: c+pow((i-av), 2), a, 0) / float(len(a)))

if __name__ == '__main__':
    print stddev(map(int, sys.argv[1:]))
