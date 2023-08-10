# Script Name   : simulate.py
# Author        : buter
# Created       : 2021-09-02
# Last Modified : 2021-09-02
# Version       : 0.1

# Modifications	:

# Description   : 模拟多个设备
from mx86 import mx86
import threading




def run(index):
    mx86('locahost', 8888, 'test'+str(index))

for i in range(1, 2000):
    mx86.log.logger.info(str(i))
    thread1 = threading.Thread(
        target=run, name='report record thread', args=(i,))
    print(thread1)
    #thread1.setDaemon(True)
    thread1.start()
