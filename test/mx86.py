# Script Name   : mx86.py
# Author        : buter
# Created       : 2021-09-02
# Last Modified : 2021-09-02
# Version       : 0.1

# Modifications	:

# Description   : 模拟MX86的mqtt协议
import subprocess
from paho.mqtt import client as mqtt_client
import random
import requests
import time
from log4p import Logger
import shutil
import threading
import demjson3


class mx86:
    log = Logger('all2.log', level='debug')
    

    def __init__(self, broker, port, name):
        self.DeviceAddr = name
        self.broker = broker
        self.port = port
        self.client_id = f'{name}aaaa'
        client = self.connect_mqtt()
        client.loop_forever()

    def publish(self, client, topic, message, desc):  #
        result = client.publish(topic, message)
        status = result[0]
        if status == 0:
            mx86.log.logger.info(f'{desc}发送成功')
        else:
            mx86.log.logger.info(f'{desc}发送失败')

    def reqtimesync0(self, client):  # 同步时间
        self.publish(client, '20180820/reqtimesync',
                     demjson3.encode({'SerialNo': 10000, 'DeviceAddr': self.DeviceAddr, 'ReqType': 0}), '同步时间')

    def reqtimesync2(self, client):  # 同步时间，可以作为心跳
        while(True):
            time.sleep(30)
            self.publish(client, '20180820/reqtimesync',
                         demjson3.encode({'SerialNo': 10000, 'DeviceAddr': self.DeviceAddr, 'ReqType': 2}), '同步时间做心跳')

    def connect_mqtt(self):
        def on_connect(client, userdata, flags, rc):
            if rc == 0:
                mx86.log.logger.info("连上MQTT代理!")
                self.run(client)
            else:
                mx86.log.logger.info("连接失败，返回 %d\n", rc)
        # Set Connecting Client ID
        client = mqtt_client.Client(self.client_id)
        client.on_connect = on_connect
        client.connect(self.broker, self.port, 30)
        return client

    def reportRecord(self, client):  # 通信记录上报
        while(True):
            time.sleep(random.randint(10, 30))
            records = []
            for i in range(1, random.randint(2, 5)):
                records.append({'IdentityType': 101,
                                'IdentityCode': f'guest{random.randint(1, 10)}',
                                'PassTime': int(time.time()),
                                'PassResult': random.randint(0, 1)})
            msg = {'SerialNo': 10001, 'DeviceAddr': self.DeviceAddr,
                   'PassRecords': records}
            self.publish( client, '20180820/reportrecord',
                         demjson3.encode(msg), '通信记录上报')

    def whitelistReply(self, client, serialNo):
        msg = {'SerialNo': serialNo, 'DeviceAddr': self.DeviceAddr, 'Result': 0}
        self.publish(client, '20180820/whitelistsync_reply',
                     demjson3.encode(msg), '白名单下发返回结果')

    def secretsyncReply(self,client, serialNo):
        msg = {'SerialNo': serialNo, 'DeviceAddr': self.DeviceAddr, 'Result': 0}
        self.publish(client, '20180820/secretsync_reply',
                demjson3.encode(msg), '同步密钥返回结果')

    def opendoorReply(self,client, serialNo):
        msg = {'SerialNo': serialNo, 'DeviceAddr': self.DeviceAddr, 'Result': 0}
        self.publish(client, '20180820/controldevice_reply',
                demjson3.encode(msg), '远程开门返回结果')

    def getstatusReply(self,client, serialNo):
        msg = {'SerialNo': serialNo, 'DeviceAddr': self.DeviceAddr,
               'DeviceMode': 'MX86', 'DeviceNetType': 1, 'DeviceVer': '6.0.0', 'OpenMode': 0, 'OpenTime': 6, 'TimeSyncInterval': 43200, 'DeviceStatus': 1, 'DeviceTime': 1596277728, 'DeviceSpace': 10000, 'DeviceWhitelistCount': 5000}
        self.publish(client, '20180820/getstatus_reply',
                demjson3.encode(msg), '获取状态返回结果')

    def setstatusReply(self,client, serialNo):
        msg = {'SerialNo': serialNo, 'DeviceAddr': self.DeviceAddr,
               'DeviceMode': 'MX86', 'DeviceNetType': 1, 'DeviceVer': '6.0.0', 'OpenMode': 0, 'OpenTime': 6, 'TimeSyncInterval': 43200, 'DeviceStatus': 1, 'DeviceTime': 1596277728, 'DeviceSpace': 10000, 'DeviceWhitelistCount': 5000}
        self.publish(client, '20180820/setstatus_reply',
                demjson3.encode(msg), '设置状态返回结果')
        mx86.log.logger.info("设置状态返回结果!")

    def timesyncReply(self,client, serialNo):
        msg = {'SerialNo': serialNo, 'DeviceAddr': self.DeviceAddr, 'Result': 0}
        self.publish(client, '20180820/timesync_reply',
                demjson3.encode(msg), '同步时间返回结果')

    def on_message(self,client, userdata, msg):
        mx86.log.logger.info(
            f"Received `{msg.payload.decode()}` from `{msg.topic}` topic")
        content = demjson3.decode(msg.payload.decode())
        serialNo = content['SerialNo']
        if(msg.topic == f'20180820/{self.DeviceAddr}/whitelistsync'):
            self.whitelistReply(client, serialNo)
        if(msg.topic == f'20180820/{self.DeviceAddr}/secretsync'):
            self.secretsyncReply(client, serialNo)
        if(msg.topic == f'20180820/{self.DeviceAddr}/controldevice'):
            self.opendoorReply(client, serialNo)
        if(msg.topic == f'20180820/{self.DeviceAddr}/getstatus'):
            self.getstatusReply(client, serialNo)
        if(msg.topic == f'20180820/{self.DeviceAddr}/setstatus'):
            self.setstatusReply(client, serialNo)
        if(msg.topic == f'20180820/{self.DeviceAddr}/timesync'):
            self.timesyncReply(client, serialNo)

    def on_disconnect(slef,client, packet, exc=None):
        mx86.log.logger.info(f"Disconnected")

    def run(self,client):
        client.on_message = self.on_message
        client.on_disconnect = self.on_disconnect
        self.reqtimesync0(client)
        thread1 = threading.Thread(
            target=self.reportRecord, name='report record thread', args=(client,))
        thread1.setDaemon(True)
        thread1.start()

        thread2 = threading.Thread(
            target=self.reqtimesync2, name='heart', args=(client,))
        thread2.setDaemon(True)
        thread2.start()

    if __name__ == '__main__':
        client = connect_mqtt()
        client.loop_forever()
