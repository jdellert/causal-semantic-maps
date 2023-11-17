# Building a semantic map from data

# Reported by Regier, Khetarpal, & Majid. "Inferring semantic maps"
# in Linguistic Typology (2013)

# Uses Angluin et al's (2010) algorithm for network inference

# Input code changed by Johannes Dellert to work with TSV files of the format
# Language <TAB> Lemma <TAB> {SENSE1, SENSE2, SENSE3, ...}

# Output code changed by Johannes Dellert to produce DOT output which can be copied
# into a file (e.g. map.dot), and visualized in GraphViz by running this command:
# $ neato -Tpdf map.dot -o map.pdf

from __future__ import generators
import networkx as nx
import xlrd
import sys

##################################################################################
# FUNCTIONS

# get all pairs of items in a sequence
def allpairs(seq):
  l = len(seq)
  for i in range(l):
    for j in range(i+1, l):
      yield seq[i], seq[j]

# angluin et al connectedness function, which we will greedily maximize
# arguments: graph g, constraint set s.
def C(g,s):
    total = 0
    for c in s:  # foreach constraint c in constraint set s
        # find num connected components in subgraph that c induces on g
        ncc = nx.number_connected_components(G.subgraph(c))
        total += (1-ncc)
    return total

# convert map into a string in DOT format for GraphViz visualization
def graphviz_output(G):
  graphviz_string = "digraph CausalGraph\n{\n  splines=true;\n"
  graphviz_string += "  " + "node [ fontname=Arial, fontcolor=blue, fontsize=20];\n"
  for n in G.nodes():
    graphviz_string += "  \"" + str(n) + "\";\n"
  graphviz_string += "subgraph undirected\n{\n  edge [dir=none];\n"
  for m in G.nodes():
    for n in G.neighbors(m):
      if str(m) < str(n):
        graphviz_string += "  \"" + str(m) + "\" -> \"" + str(n) + "\" "
        graphviz_string += " [color=\"#cc0000ff\",penwidth=\"5\"];\n"
  graphviz_string += "  }\n}\n"
  return graphviz_string

##################################################################################
#################################### MAIN ########################################
##################################################################################

# PARSE COMMAND LINE ARGUMENTS
if len(sys.argv) == 2:
  inputFname = sys.argv[1]
else:
  print("\nCORRECT USAGE: regier-adapted.py isolecticsets.tsv\n")

##################################################################################
# READ IN DATA FROM ISOLECTIC SET FILE

# set up arrays to hold data
T=[] # senses for each term.
L=[] # language name for each term in T.
N=[] # name of each term in T.

with open(sys.argv[1], "r", encoding="utf-8") as isolectic_set_file:
  for line in isolectic_set_file:
    line = line.strip()
    fields = line.split("\t")
    L.append(fields[0])
    N.append(fields[1])
    senses = fields[2][1:-1].split(", ")
    T.append(senses)

##################################################################################
# CREATE INITIAL GRAPH

# graph G: add each term's nodes, no edges in graph yet.
G = nx.Graph()  # create empty graph (undirected)
PossE = []      # list of possible edges, filled below
for t in T:
  # add all nodes in t, if not already in graph
  for n in t:
    if (not G.has_node(n)):
      G.add_node(n)
  # add to PossE a link between each pair of nodes in t
  # adding a link between every node in G is needless and slower
  for pair in allpairs(t):
    u = pair[0]
    v = pair[1]
    if (not (((u,v) in PossE) or ((v,u) in PossE))):
      PossE.append((u,v))

##################################################################################
# MAIN LOOP

objfn = C(G,T)
while (objfn < 0):
    print("objective fn is currently " + str(objfn))
    max_score = 0
    # choose next edge greedily: the one that increases objfn the most
    for e in PossE:
        # temporarily add e to graph G
        G.add_edge(*e)
        score = C(G,T) - objfn
        G.remove_edge(*e)
        if (score > max_score):
            max_score = score
            max_edge = e
    print("adding " + str(max_edge) +  " with score " + str(max_score))
    G.add_edge(*max_edge)
    PossE.remove(max_edge)   # remove max_edge fr PossE
    objfn = C(G,T)

##################################################################################
# PRINT RESULTS TO STDOUT (FOLLOWED BY GRAPHVIZ REPRESENTATION)

print("\nFINAL GRAPH:")
print("\nList of nodes:\n" + str(G.nodes()))
print("\nTotal number of edges:" + str(len(G.edges())))
print("\nNodes and their direct neighbors:")
for n in G.nodes():
  print(str(n) + ": " + str(set(G.neighbors(n))))
print("\nDOT OUTPUT:")
print(graphviz_output(G))
