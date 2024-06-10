import os, sys, random, time
import logging
sys.path.append("../")


from util import *

logger = logging.getlogger(__name__)
logger.setlevel(logging.DEBUG)
logger_ch = logging.StreamHandler()
logger_ch.setLevel(logging.DEBUG)
logger_ch.setFormatter(logging.Formatter('%(asctime)s[%(levelname)s][%(lineno)s:%(funcName)s]||%(message)s', datefmt= '%Y-%m-%d %H:%M:%S'))
logger.addHandler(logger_ch)
RANDOM_SEED = 0


class RideHailSim: 
  '''A ride hailing simulation'''
      
    def __init__(self, global_flag="global", time_interval=10, probability=1.0/28, real_orders=""):
   
      
#New functions




#Test functionalities

