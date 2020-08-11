#!/usr/bin/env python3

import pandas as pd
import json
import gc
from nltk.corpus import wordnet
from collections import defaultdict
df = pd.read_csv("./word-by-frequency.txt", sep="\t", header=None, index_col=0)
gc.collect()
with open("./word2sent.json", encoding="ascii", errors="ignore") as f:
    examples = json.load(f)

word_set = set(df[1])
examples = {k: v for k, v in examples.items() if k in word_set}
gc.collect()
word_syn = defaultdict(list)
word_ant = defaultdict(list)
for word in list(df[1]):
    for syn in wordnet.synsets(word):
        if word in examples:
            examples[word].append(syn.examples())
        for lemma in syn.lemmas():
            word_syn[word].append(lemma.name())
            if lemma.antonyms():
                for ant in lemma.antonyms():
                    word_ant[word].append(ant.name())

with open("./synonyms.json", "w") as f:
    json.dump(word_syn, f, indent=2)


with open("./antonyms.json", "w") as f:
    json.dump(word_ant, f, indent=2)

with open("./examples.json", "w") as f:
    json.dump(examples, f, indent=2)
