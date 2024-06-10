"""version:
pandas = 1.0.1
numpy = 1.18.1
matplotlib = 3.1.3
"""
import pandas as pd
import numpy as np
import random
import matplotlib.pyplot as plt
import pickle

# print graphs for reward, distance, etc. / averages episodes

#rewards = pd.read_pickle(r'C:/Users/tdavi/Downloads/David191020/reward10k_r_dqn.pkl')
rewards = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/reward50kdqn_2701_2_highState.pkl')
rewards.columns = ["reward"]
rewards = rewards["reward"].tolist()

print(np.var(rewards))


duration = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/duration50kdqn_2701_2_lowState.pkl')
duration.columns = ["duration"]
duration = duration["duration"].tolist()


over = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/over50kdqn_2701_2_lowState.pkl')
over.columns = ["over"]
over = over["over"].tolist()


job = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/jobs50kdqn_2701_2_lowState.pkl')
job.columns = ["job"]
job = job["job"].tolist()


distance = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kdqn_2701_2_lowState.pkl')
distance.columns = ["distance"]
distance = distance["distance"].tolist()


charge = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/charges50kql_2701_2.pkl')
charge.columns = ["charge"]
charge = charge["charge"].tolist()

#rewards = rewards[:100000]
#print(rewards)


sub2 = []
step = 1000
for i, _ in enumerate(rewards[::step]):
     sub_list = rewards[i*10:] if (i+1)*10 > len(rewards) else rewards[i*10:(i+1)*10]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub2.append(sub)


plt.plot(np.arange(len(sub2)), sub2, color= "red")
plt.ylabel('Total reward')
plt.xlabel('Episodes')
plt.show()




sub3 = []
step = 1000
for i, _ in enumerate(duration[::step]):
     sub_list = duration[i*10:] if (i+1)*10 > len(duration) else duration[i*10:(i+1)*10]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub3.append(sub)

plt.plot(np.arange(len(sub3)), sub3)
plt.ylabel('Total duration')
plt.xlabel('Episodes')
plt.show()




sub4 = []
step = 1000
for i, _ in enumerate(over[::step]):
     sub_list = over[i*10:] if (i+1)*10 > len(over) else over[i*10:(i+1)*10]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub4.append(sub)

plt.plot(np.arange(len(sub4)), sub4)
plt.ylabel('Total over')
plt.xlabel('Episodes')
plt.show()




sub5 = []
step = 1000
for i, _ in enumerate(job[::step]):
     sub_list = job[i*10:] if (i+1)*10 > len(job) else job[i*10:(i+1)*10]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub5.append(sub)


plt.plot(np.arange(len(sub5)), sub5)
plt.ylabel('Total job')
plt.xlabel('Episodes')
plt.show()



sub6 = []
step = 1000
for i, _ in enumerate(distance[::step]):
     sub_list = distance[i*10:] if (i+1)*10 > len(distance) else distance[i*10:(i+1)*10]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub6.append(sub)


plt.plot(np.arange(len(sub6)), sub6)
plt.ylabel('Total distance')
plt.xlabel('Episodes')
plt.show()




sub7 = []
step = 1000
for i, _ in enumerate(charge[::step]):
     sub_list = charge[i*10:] if (i+1)*10 > len(charge) else charge[i*10:(i+1)*10]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub7.append(sub)

plt.plot(np.arange(len(sub7)), sub7)
plt.ylabel('Total charge')
plt.xlabel('Episodes')
plt.show()

