"""version:
pandas = 1.0.1
"""

from brain import DeepQNetwork
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


def update():
    """ creating the RL between brain and environment"""

    env.create_trips()
    step = 0
    for episode in range(50000):
        env.refresh_trips()
        observation = env.reset()
        total_reward = 0
        total_duration = 0
        total_over = 0
        total_distance = 0
        total_jobs = 0
        total_charges = 0
        #print("Episode" + str(episode))
    
        while True:
            #print("Step:" + str(step))
            #print(env.state)

            #RL choose action based on observation
            action = RL.choose_action(observation)

            #RL take action and get next observation and reward
            observation_, reward, done = env.decide(action)

            RL.store_transition(observation, action, reward, observation_)

            if (step > 200) and (step % 5 == 0):
                RL.learn()

            # swap observation
            observation = observation_

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
                # evaluation list for total episode
                total_reward_list.append(total_reward)
                total_duration_list.append(total_duration)
                total_over_list.append(total_over)
                total_distance_list.append(total_distance)
                total_jobs_list.append(total_jobs)
                total_charges_list.append(total_charges)

                # reset variables for new episode
                total_reward = 0
                total_duration = 0
                total_over = 0
                total_jobs = 0
                total_distance = 0
                total_charges = 0
                break

            step = step + 1

    df_reward = pd.DataFrame(total_reward_list)
    df_duration = pd.DataFrame(total_duration_list)
    df_over = pd.DataFrame(total_over_list)
    df_jobs = pd.DataFrame(total_jobs_list)
    df_distance = pd.DataFrame(total_distance_list)
    df_charges = pd.DataFrame(total_charges_list)

    # save with pickle
    df_reward.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/reward50kdqn_2701_2s_lowState.pkl")
    df_duration.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/duration50kdqn_2701_2s_lowState.pkl")
    df_over.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/over50kdqn_2701_2s_lowState.pkl")
    df_jobs.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/jobs50kdqn_2701_2s_lowState.pkl")
    df_distance.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kdqn_2701_2s_lowState.pkl")
    df_charges.to_pickle("C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/charges50kdqn_2701_2s_lowState.pkl")

    
if __name__ == "__main__":
    env = Vehicle()        
    RL = DeepQNetwork(env.n_actions, env.n_features,
                    learning_rate=0.01,
                    reward_decay=0.9,
                    e_greedy=0.9,
                    replace_target_iter=200,
                    memory_size=2000,
                    # output_graph=True
                     )
    start_time = time.time()
    update()
    print("--- %s seconds ---" % (time.time() - start_time))
    RL.plot_total_reward(total_reward_list)
