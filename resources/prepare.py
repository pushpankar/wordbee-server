#!/usr/bin/env python3

import pandas as pd
import json
from nltk.corpus import wordnet
from collections import defaultdict
df = pd.read_csv("./word-by-frequency.txt", sep="\t", header=None, index_col=0)

word_syn = defaultdict(list)
word_ant = defaultdict(list)
for word in list(df[1]):
    for syn in wordnet.synsets(word):
        for lemma in syn.lemmas():
            word_syn[word].append(lemma.name())
            if lemma.antonyms():
                for ant in lemma.antonyms():
                    word_ant[word].append(ant.name())

with open("./synonyms.json", "w") as f:
    json.dump(word_syn, f, indent=2)


with open("./antonyms.json", "w") as f:
    json.dump(word_ant, f, indent=2)
