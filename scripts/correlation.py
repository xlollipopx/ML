import pandas as pd
import numpy as np 
import matplotlib as plt
import matplotlib.pyplot as pyplot
import seaborn as sns




pd.set_option('display.max_columns', 19)
sns.set(style="white")
sns.set(color_codes=True)

df = pd.read_csv('../2008.csv', sep=",")
corr = df.corr()
f, ax = plt.pyplot.subplots(figsize=(20, 20))

cmap = sns.diverging_palette(220, 10, as_cmap=True)

sns.heatmap(corr, cmap=cmap,  center=0, square=True, linewidths=.9, cbar_kws={"shrink": .5})
pyplot.show()

