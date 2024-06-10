import pandas as pd
import numpy as np
import random
import matplotlib.pyplot as plt
import pickle


""" analyze distance and duration by including the number of jobs accepted """


# job for all 3 algos

job_q = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/jobs50kql_2701_2.pkl')
job_q.columns = ["job"]
job_q = job_q["job"].tolist()
job_q =np.array(job_q, dtype=np.float)

job_l = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/jobs50kdqn_2701_2_lowState.pkl')
job_l.columns = ["job"]
job_l = job_l["job"].tolist()
job_l = np.array(job_l, dtype=np.float)

job_h = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/jobs50kdqn_2701_2_highState.pkl')
job_h.columns = ["job"]
job_h = job_h["job"].tolist()
job_h =np.array(job_h, dtype=np.float)


# duration for all 3 algos

duration_q = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/duration50kql_2701_2.pkl')
duration_q.columns = ["duration"]
duration_q = duration_q["duration"].tolist()
duration_q = np.array(duration_q, dtype=np.float)

duration_l = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/duration50kdqn_2701_2_lowState.pkl')
duration_l.columns = ["duration"]
duration_l = duration_l["duration"].tolist()
duration_l = np.array(duration_l, dtype=np.float)

duration_h = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/duration50kdqn_2701_2_highState.pkl')
duration_h.columns = ["duration"]
duration_h = duration_h["duration"].tolist()
duration_h = np.array(duration_h, dtype=np.float)

# distance for all 3 algos

distance_q = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kql_2701_2.pkl')
distance_q.columns = ["distance"]
distance_q = distance_q["distance"].tolist()
distance_q = np.array(distance_q, dtype=np.float)


distance_l = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kdqn_2701_2_lowState.pkl')
distance_l.columns = ["distance"]
distance_l = distance_l["distance"].tolist()
distance_l = np.array(distance_l, dtype=np.float)


distance_h = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kdqn_2701_2_highState.pkl')
distance_h.columns = ["distance"]
distance_h = distance_h["distance"].tolist()
distance_h = np.array(distance_h, dtype=np.float)


# create new values including jobs in distance and duration

duration2_q = [x/y if y else 0 for x,y in zip(duration_q,job_q)]
duration2_l = [x/y if y else 0 for x,y in zip(duration_l,job_l)]
duration2_h = [x/y if y else 0 for x,y in zip(duration_h,job_h)]


average1 = sum(duration2_q[:1000])/1000
print(average1)

average2 = sum(duration2_q[49000:])/1000
print(average2)

average1 = sum(duration2_l[:1000])/1000
print(average1)

average2 = sum(duration2_l[49000:])/1000
print(average2)

average1 = sum(duration2_h[:1000])/1000
print(average1)

average2 = sum(duration2_h[49000:])/1000
print(average2)




sub2 = []
step = 1000
for i, _ in enumerate(duration2_q[::step]):
     sub_list = duration2_q[i*1000:] if (i+1)*1000 > len(duration2_q) else duration2_q[i*1000:(i+1)*1000]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub2.append(sub)

sub3 = []
step = 1000
for i, _ in enumerate(duration2_l[::step]):
     sub_list = duration2_l[i*1000:] if (i+1)*1000 > len(duration2_l) else duration2_l[i*1000:(i+1)*1000]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub3.append(sub)

sub4 = []
step = 1000
for i, _ in enumerate(duration2_h[::step]):
     sub_list = duration2_h[i*1000:] if (i+1)*1000 > len(duration2_h) else duration2_h[i*1000:(i+1)*1000]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub4.append(sub)

plt.plot(np.arange(len(sub2)), sub2, color = "red", label = "Q-Learning")
plt.plot(np.arange(len(sub3)), sub3, color = "green", label = "DQN low")
plt.plot(np.arange(len(sub4)), sub4, color = "blue", label = "DQN high")

plt.ylabel('duration / jobs')
plt.xlabel('Episodes (in 1000)')
plt.legend()
plt.show()




# distance 2
distance2_q = [x/y if y else 0 for x,y in zip(distance_q,job_q)]
distance2_l = [x/y if y else 0 for x,y in zip(distance_l,job_l)]
distance2_h = [x/y if y else 0 for x,y in zip(distance_h,job_h)]


# print distance /jobs

sub5 = []
step = 1000
for i, _ in enumerate(distance2_q[::step]):
     sub_list = distance2_q[i*1000:] if (i+1)*1000 > len(distance2_q) else distance2_q[i*1000:(i+1)*1000] 
     sub = (sum(sub_list)/float(len(sub_list)))
     sub5.append(sub)

sub6 = []
step = 1000
for i, _ in enumerate(distance2_l[::step]):
     sub_list = distance2_l[i*1000:] if (i+1)*1000 > len(distance2_l) else distance2_l[i*1000:(i+1)*1000]  
     sub = (sum(sub_list)/float(len(sub_list)))
     sub6.append(sub)

sub7 = []
step = 1000
for i, _ in enumerate(distance2_h[::step]):
     sub_list = distance2_h[i*1000:] if (i+1)*1000 > len(distance2_h) else distance2_h[i*1000:(i+1)*1000]  
     sub = (sum(sub_list)/float(len(sub_list)))
     sub7.append(sub)

plt.plot(np.arange(len(sub5)), sub5, color = "red", label = "Q-Learning")
plt.plot(np.arange(len(sub6)), sub6, color = "green", label = "DQN low")
plt.plot(np.arange(len(sub7)), sub7, color = "blue", label = "DQN high")

plt.ylabel('distance / jobs')
plt.xlabel('Episodes (in 1000)')
plt.legend()
plt.show()



# average reward per committed trip

commit_reward_q = [10*x/y if y else 0 for x,y in zip(distance2_q,duration2_q)]
commit_reward_l = [10*x/y if y else 0 for x,y in zip(distance2_l,duration2_l)]
commit_reward_h = [10*x/y if y else 0 for x,y in zip(distance2_h,duration2_h)]


sub2 = []
step = 1000
for i, _ in enumerate(commit_reward_q[::step]):
     sub_list = commit_reward_q[i*1000:] if (i+1)*1000 > len(commit_reward_q) else commit_reward_q[i*1000:(i+1)*1000]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub2.append(sub)

print(sub2)

sub3 = []
step = 1000
for i, _ in enumerate(commit_reward_l[::step]):
     sub_list = commit_reward_l[i*1000:] if (i+1)*1000 > len(commit_reward_l) else commit_reward_l[i*1000:(i+1)*1000]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub3.append(sub)

sub4 = []
step = 1000
for i, _ in enumerate(commit_reward_h[::step]):
     sub_list = commit_reward_h[i*1000:] if (i+1)*1000 > len(commit_reward_h) else commit_reward_h[i*1000:(i+1)*1000]  # Condition if the len(my_list) % step != 0
     sub = (sum(sub_list)/float(len(sub_list)))
     sub4.append(sub)

plt.plot(np.arange(len(sub2)), sub2, color = "red", label = "Q-Learning")
plt.plot(np.arange(len(sub3)), sub3, color = "green", label = "DQN low")
plt.plot(np.arange(len(sub4)), sub4, color = "blue", label = "DQN high")

plt.ylabel('reward / jobs')
plt.xlabel('Episodes (in 1000)')
plt.legend()
plt.show()


# print start and end values of reward/jobs

print("--------")
print("reward / jobs")
print("-----------------")
average1 = sum(commit_reward_q[:1000])/1000
print(average1)

average2 = sum(commit_reward_q[49000:])/1000
print(average2)

average1 = sum(commit_reward_h[:1000])/1000
print(average1)

average2 = sum(commit_reward_h[49000:])/1000
print(average2)

average1 = sum(commit_reward_l[:1000])/1000
print(average1)

average2 = sum(commit_reward_l[49000:])/1000
print(average2)



# print start and end values of distance / jobs

print("--------")
average1 = sum(distance2_q[:1000])/1000
print(average1)

average2 = sum(distance2_q[49000:])/1000
print(average2)

average1 = sum(distance2_l[:1000])/1000
print(average1)

average2 = sum(distance2_l[49000:])/1000
print(average2)

average1 = sum(distance2_h[:1000])/1000
print(average1)

average2 = sum(distance2_h[49000:])/1000
print(average2)

