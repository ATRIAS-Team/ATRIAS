import pandas as pd
import numpy as np
import random
import matplotlib.pyplot as plt
import pickle

""" compare graphs of different algorithm implementations graphically in 1000 steps. Can be used for all other evaluation variables """

rewards_dqnh = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kdqn_2701_2_highState.pkl')
rewards_dqnh.columns = ["reward"]
rewards_dqnh = rewards_dqnh["reward"].tolist()

rewards_dqnl = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kdqn_2701_2_lowState.pkl')
rewards_dqnl.columns = ["reward"]
rewards_dqnl = rewards_dqnl["reward"].tolist()

rewards_ql = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kql_2701_2.pkl')
rewards_ql.columns = ["reward"]
rewards_ql = rewards_ql["reward"].tolist()

sub2 = []
step = 1000
for i, _ in enumerate(rewards_ql[::step]):
     sub_list = rewards_ql[i*1000:] if (i+1)*1000 > len(rewards_ql) else rewards_ql[i*1000:(i+1)*1000] 
     sub = (sum(sub_list)/float(len(sub_list)))
     sub2.append(sub)


sub3 = []
step = 1000
for i, _ in enumerate(rewards_dqnh[::step]):
     sub_list = rewards_dqnh[i*1000:] if (i+1)*1000 > len(rewards_dqnh) else rewards_dqnh[i*1000:(i+1)*1000] 
     sub = (sum(sub_list)/float(len(sub_list)))
     sub3.append(sub)

sub4 = []
step = 1000
for i, _ in enumerate(rewards_dqnl[::step]):
     sub_list = rewards_dqnl[i*1000:] if (i+1)*1000 > len(rewards_dqnl) else rewards_dqnl[i*1000:(i+1)*1000]  
     sub = (sum(sub_list)/float(len(sub_list)))
     sub4.append(sub)


plt.plot(np.arange(len(sub2)), sub2, color = "red", label = "Q-Learning")
plt.plot(np.arange(len(sub4)), sub4, color = "green", label = "DQN low")
plt.plot(np.arange(len(sub3)), sub3, color = "blue", label = "DQN high")

plt.ylabel('Total distance')
plt.xlabel('Episodes (in 1000)')
plt.legend()
plt.show()
