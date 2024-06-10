import pandas as pd
import numpy as np
import random
import matplotlib.pyplot as plt
import pickle

""" create graphs comparing rewards of the different implementations in the first 1000 episodes in 100 steps"""

rewards_dqnh = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/over50kdqn_2701_2_highState.pkl')
rewards_dqnh.columns = ["reward"]
rewards_dqnh = rewards_dqnh["reward"].tolist()
rewards_dqnh = rewards_dqnh[:1000]

rewards_dqnl = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/over50kdqn_2701_2_lowState.pkl')
rewards_dqnl.columns = ["reward"]
rewards_dqnl = rewards_dqnl["reward"].tolist()
rewards_dqnl = rewards_dqnl[:1000]

rewards_ql = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/over50kql_2701_2.pkl')
rewards_ql.columns = ["reward"]
rewards_ql = rewards_ql["reward"].tolist()
rewards_ql  = rewards_ql[:1000]

# print variances of all implementations 

print(np.var(rewards_dqnh))
print(np.var(rewards_dqnl))
print(np.var(rewards_ql))


sub2 = []
step = 100
for i, _ in enumerate(rewards_ql[::step]):
     sub_list = rewards_ql[i*100:] if (i+1)*100 > len(rewards_ql) else rewards_ql[i*100:(i+1)*100]  
     sub = (sum(sub_list)/float(len(sub_list)))
     sub2.append(sub)


sub3 = []
step = 100
for i, _ in enumerate(rewards_dqnh[::step]):
     sub_list = rewards_dqnh[i*100:] if (i+1)*100 > len(rewards_dqnh) else rewards_dqnh[i*100:(i+1)*100]  
     sub = (sum(sub_list)/float(len(sub_list)))
     sub3.append(sub)

sub4 = []
step = 100
for i, _ in enumerate(rewards_dqnl[::step]):
     sub_list = rewards_dqnl[i*100:] if (i+1)*100 > len(rewards_dqnl) else rewards_dqnl[i*100:(i+1)*100]  
     sub = (sum(sub_list)/float(len(sub_list)))
     sub4.append(sub)


plt.plot(np.arange(len(sub2)), sub2, color = "red", label = "Q-Learning")
plt.plot(np.arange(len(sub4)), sub4, color = "green", label = "DQN low")
plt.plot(np.arange(len(sub3)), sub3, color = "blue", label = "DQN high")

plt.ylabel('Total over')
plt.xlabel('Episodes (in 100)')
plt.legend()
plt.show()


