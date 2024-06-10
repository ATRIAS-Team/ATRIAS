"""version:
pandas = 1.0.1
"""
from brain import QLearningTable
from environment import Vehicle
import pandas as pd
import time

# for evaluation purpose
total_reward_list = []
total_duration_list = []
total_over_list = []
total_jobs_list = []
total_distance_list = []
total_charges_list = []

def update ():
    """ RL loop between brain and environment"""
    
    env.create_trips()
    for episode in range(50000):
        env.refresh_trips()
        observation = env.reset()
        total_reward = 0
        total_duration = 0
        total_over = 0
        total_distance = 0
        total_jobs = 0
        total_charges = 0
    
        while True:
            #RL choose action based on observation
            action = RL.choose_action(str(observation))
            print(action)

            #RL take action and get next observation and reward
            observation_, reward, done = env.decide(action)

            #RL learn from this transition
            RL.learn(str(observation), action, reward, str(observation_))

            # swap observation
            observation = observation_

            # for evaluation
            duration = env.duration
            over = env.over
            job = env.job
            distance = env.distance
            charge = env.charge

            total_reward = total_reward + reward
            total_duration = total_duration + duration
            total_over = total_over + over 
            total_distance = total_distance + distance
            total_jobs = total_jobs + job
            total_charges = total_charges + charge

            # break while loop when end of this episode
            if done:
                print("total reward: ", total_reward)
                total_reward_list.append(total_reward)
                total_duration_list.append(total_duration)
                total_over_list.append(total_over)
                total_distance_list.append(total_distance)
                total_jobs_list.append(total_jobs)
                total_charges_list.append(total_charges)

                # reset eval variables for next episode
                total_reward = 0
                total_duration = 0
                total_over = 0
                total_jobs = 0
                total_distance = 0
                total_charges = 0
                break

    df_reward = pd.DataFrame(total_reward_list)
    df_duration = pd.DataFrame(total_duration_list)
    df_over = pd.DataFrame(total_over_list)
    df_jobs = pd.DataFrame(total_jobs_list)
    df_distance = pd.DataFrame(total_distance_list)
    df_charges = pd.DataFrame(total_charges_list)

    # save results with pickle
    df_reward.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/reward50kql_2701_2s.pkl")
    df_duration.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/duration50kql_2701_2s.pkl")
    df_over.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/over50kql_2701_2s.pkl")
    df_jobs.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/jobs50kql_2701_2s.pkl")
    df_distance.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kql_2701_2s.pkl")
    df_charges.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/charges50kql_2701_2s.pkl")
    
    
if __name__ == "__main__":
    env = Vehicle()               
    RL = QLearningTable(actions=["commit", "negotiate"])
    start_time = time.time()
    update()
    print("--- %s seconds ---" % (time.time() - start_time))
    RL.plot_total_reward(total_reward_list)
