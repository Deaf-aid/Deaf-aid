from cmath import log10
from math import log2
from tkinter import *
import scipy.signal as signal
import numpy as np
import copy
import serial
import threading
from EEMD_BSS import signal_eemd, fastica
from itertools import combinations
from scipy.optimize import leastsq
import math
from scipy.fftpack import fft

MAX_NUM = 8192
gry_x = [0 for i in range(MAX_NUM)]
gry_y = [0 for i in range(MAX_NUM)]
gry_z = [0 for i in range(MAX_NUM)]

total_num = 0
ser = serial.Serial("COM5",115200)
data_LSB = 0.06
sampling_rate = 100
IDrange = [23110, 25910]
for i in range(2):
    if IDrange[i]%sampling_rate>sampling_rate/2:
        IDrange[i] = sampling_rate - IDrange[i]%sampling_rate
    else:
        IDrange[i] = IDrange[i]%sampling_rate
def read_gyr():
    global gry_x, gry_y, gry_z, total_num
    
    while (1):
        s = ser.readline().decode('gbk')
        data = str(s)   
        gry = data.split(' ')
        if len(gry)==7:          
            gry_x.append(float(gry[0]))
            gry_y.append(float(gry[2]))
            gry_z.append(float(gry[4]))
            del gry_x[0]
            del gry_y[0]
            del gry_z[0]
            total_num += 1
def mean_filter(data, length):
    data = [0]*int(length/2) + data + [0]*(length-int(length/2)-1)
    res = []
    for i in range(len(data)-length+1):
        res += [sum(data[i:i+length])/length]

    return res

def residual(p, x, y):
    a, b, c, d, e, f=p 
    return a*x*x+b*x*y+c*y*y+d*x+e*y+f

def normal_style(paras):
    paras=paras/paras[5]
    A,B,C,D,E=paras[:5]
       
    x0=(B*E-2*C*D)/(4*A*C-B**2)
    y0=(B*D-2*A*E)/(4*A*C-B**2)
  
    a= 2*np.sqrt((2*A*(x0**2)+2*C*(y0**2)+2*B*x0*y0-2)/(A+C+np.sqrt(((A-C)**2+B**2))))
    b= 2*np.sqrt((2*A*(x0**2)+2*C*(y0**2)+2*B*x0*y0-2)/(A+C-np.sqrt(((A-C)**2+B**2))))
    
    theta=0.5 * np.arctan(B/(A-C))
   
    return x0,y0,a,b,theta

def motion_removal(data1, data2):
    E_IMFS1, Num1 = signal_eemd(data1, MAX_NUM/sampling_rate)
    E_IMFS2, Num2 = signal_eemd(data2, MAX_NUM/sampling_rate)
    E_IMFS1 = fastica(E_IMFS1, Num1)
    E_IMFS2 = fastica(E_IMFS2, Num1)
    seq1 = []
    seq2 = []
    for i in range(1, len(E_IMFS1)):
        for c in combinations(E_IMFS1, i):
            tmp = np.sum(c, axis=0).tolist()
            seq1.append(tmp)
    for i in range(1, len(E_IMFS2)):
        for c in combinations(E_IMFS2, i):
            tmp = np.sum(c, axis=0).tolist()
            seq2.append(tmp)


    max_area = 0
    index1 = -1
    index2 = -1

    for i in range(len(seq1)):
        for j in range(len(seq2)):
            s1 = copy.deepcopy(seq1[i])
            s2 = copy.deepcopy(seq2[j])
            for k in range(len(s1)):
           
                if (s1[k]>=6*data_LSB and s1[k]<=6*data_LSB) and (s2[k]>=6*data_LSB and s2[k]<=6*data_LSB):
                    del s1[k], s2[k]
                paras = leastsq(residual, [1, 1, 1, 1, 1, 1], args=(s1, s2))
                x0, y0, a, b ,theta = normal_style(paras[0]/paras[0][0])
                mse = np.sum(residual(paras[0]/paras[0][0], s1, s2))/len(s1)
                if mse<0.01:
                    area = math.pi*a*b 
                    if area>max_area:
                        max_area=area 
                        index1 = i
                        index2 = j

    return seq1[index1], seq2[index2]


def ChooseAxis(x, y, z):
    x_var = np.var(x)
    y_var = np.var(y)
    z_var = np.var(z)
    x_mean = np.mean(x)
    y_mean = np.mean(y)
    z_mean = np.mean(z)
    x_motion = x_mean>=-6*data_LSB and x_mean<=6*data_LSB
    y_motion = y_mean>=-6*data_LSB and y_mean<=6*data_LSB
    z_motion = z_mean>=-6*data_LSB and z_mean<=6*data_LSB
    if x_motion+y_motion+z_motion == 3:
        temp = min(x_var, y_var, z_var)
        if temp==x_var:
            axis1 = y 
            axis2 = z
        elif temp==y_var:
            axis1 = x 
            axis2 = z
        else:
            axis1 = x 
            axis2 = y
    elif x_motion+y_motion+z_motion == 2:
        if not x_motion:
            axis1 = y
            axis2 = z
        if not y_motion:
            axis1 = x
            axis2 = z
        if not z_motion:
            axis1 = x
            axis2 = y
    elif x_motion+y_motion+z_motion == 1:
        if x_motion:
            axis1 = x
            if y_var >= z_var:
                axis2 = y 
            else:
                axis2 = z 
        if y_motion:
            axis1 = y
            if x_var >= z_var:
                axis2 = x 
            else:
                axis2 = z 
        if z_motion:
            axis1 = z
            if x_var >= y_var:
                axis2 = x
            else:
                axis2 = y
        
    else:
        temp = min(x_var, y_var, z_var)
        if temp==x_var:
            axis1 = y 
            axis2 = z
        elif temp==y_var:
            axis1 = x 
            axis2 = z
        else:
            axis1 = x 
            axis2 = y
        
        axis1, axis2 = motion_removal(axis1, axis2)
    return axis1, axis2

def MaxEntropy(data):
    r = data_LSB
    K = np.ceil(max(data)/r)
   
    p = [0] * K
    for i in range(len(data)):
        for j in range(K):
            if data[i]==0:
                p[0] += 1
            if data[i]>j*r and data[i]<=(j+1)*r:
                p[j] += 1  
                break

    p /= len(data)

    H = [0] * K
    for k in range(K):
        H0 = 0
        H1 = 0
        p0_total = sum(p[0:k+1])
        p1_total = sum(p[k+1:K])
        for i in range(k+1):
            H0 += p(i)/p0_total * log2(p(i)/p0_total)
        for i in range(k+1, K):
            H1 += p(i)/p1_total * log2(p(i)/p1_total)
        H[k] = -H0-H1

    maxH = max(H)
    threshold = (H.index(maxH)+1)*r 

    return threshold

def preprocess(x, y, z):

    axis1, axis2 = ChooseAxis(x, y, z)
    
    axis = np.multiply(axis1, axis2)
    data = mean_filter(axis, 5)
    data_abs = abs(data)
    res = []
    for i in range(MAX_NUM/(sampling_rate*0.5)):
        data_tmp = data_abs[i*sampling_rate*0.5:(i+1)*sampling_rate*0.5]
        threshold = MaxEntropy(data_tmp)
        
        for d in data_tmp:
            if d<=threshold:
                res += [0]
            else:
                res += [1]
    return data, res       

def decode():
    global gry_x, gry_y, gry_z, total_num
    num = total_num
    x = copy.deepcopy(gry_x)
    y = copy.deepcopy(gry_y)
    z = copy.deepcopy(gry_z)

    data, binary_seq = preprocess(x, y, z)
    char = ""
    index = 0
    status = 0
    begin = False
    zero_count = 0
    while True:
        #ID range judge
        while True:
            try:
                one_index = binary_seq.index(1, index)
                index = one_index
                zero_index = binary_seq.index(0, index)
                index = zero_index
                if zero_index - one_index >= sampling_rate*(1.5 - 0.1) and zero_index - one_index <= sampling_rate*(1.5 + 0.1):
                    N = 0
                    
                    for i in range(20):
                        if 2**i > (zero_index - one_index)/3:
                            N = 2**i
                            break 
                    count = 0
                    f = np.arange(N/2) / N * sampling_rate
                    for i in range(3):
                        fft_y=fft(data[one_index+i*N:zero_index+(i+1)*N]) 
                        fft_abs = np.abs(fft_y)/N
                        fft_abs = fft_abs.tolist()
                        fft_abs[fft_abs>0.2] = 1
                        fft_abs[fft_abs<=0.2] = 0
                        begin = 0
                        num = 1
                        try:
                            while True:
                                index = fft_abs.index(num, begin)
                                if num==1:
                                    if f[index]>=IDrange[0]-sampling_rate/N and f[index]<=IDrange[0]+sampling_rate/N:
                                        count += 1
                                elif num==0:
                                    if f[index]>=IDrange[1]-sampling_rate/N and f[index]<=IDrange[1]+sampling_rate/N:
                                        count += 1
                                num = 1-num 
                                begin = index
                                
                        except ValueError:
                            continue
                    if count>=4:
                        break
            except ValueError:
                while total_num - num < MAX_NUM:
                    continue
                num = total_num
                x = copy.deepcopy(gry_x)
                y = copy.deepcopy(gry_y)
                z = copy.deepcopy(gry_z)

                data, binary_seq = preprocess(x, y, z)
                index = 0
        decode_data = []
        while char != "EOF":
            try:
                if status == 0:
                    one_index = binary_seq.index(1, index)
                    index = one_index
                    status = 1
                elif status == 1:
                    zero_index = binary_seq.index(0, index)
                    index = zero_index
                    status = 2
                elif status == 2:
                    one_index = binary_seq.index(1, index)
                    index = one_index
                    zero_count += one_index-zero_index
                    if zero_count >17 and zero_count <=27:
                        status = 3
                    elif zero_count >0 and zero_count <=7:
                        if begin==True:
                            decode_data += [1]
                            
                        status=1
                    elif zero_count >7 and zero_count <=17:
                        if begin==True:
                            decode_data+= [0]
                            
                        status=1
                    elif zero_count > 27 and zero_count<= 37:
                        char = "EOF"
                    else:
                        status=1
                    zero_count = 0
                elif status == 3:
                    zero_index = binary_seq.index(0, index)
                    index = zero_index
                    status = 4
                elif status == 4:
                    one_index = binary_seq.index(1, index)
                    index = one_index
                    zero_count += one_index-zero_index
                    if zero_count >0 and zero_count <=7:
                        
                        begin = True
                    status = 1
                    zero_count = 0
            except ValueError:
                if status ==2 or status ==4:
                    zero_count = len(binary_seq)-zero_index
                while total_num - num < MAX_NUM:
                    continue
                num = total_num
                x = copy.deepcopy(gry_x)
                y = copy.deepcopy(gry_y)
                z = copy.deepcopy(gry_z)

                data, binary_seq = preprocess(x, y, z)
                index = 0
        decode_data = np.array(decode_data).reshape(16, 8)
        decode_data = np.transpose(decode_data, [1, 0])
        decode_data = decode_data.reshape(-1, 1)
        decode_data = decode_data[0]
        for d in decode_data:
            txt.insert("insert", "0"+d)

root = Tk()
root.geometry('460x240')
root.title('receiver')

lb1 = Label(root, text="receive data:")
lb1.place(relx=0.2, rely=0.1, relwidth=0.2, relheight=0.1)

txt = Text(root)
txt.place(relx=0.2, rely=0.2, relwidth=0.6,relheight=0.3)

send_btn = Button(root, text="receive", command=decode)
send_btn.place(relx=0.4, rely=0.6, relwidth=0.2, relheight=0.1)

t1 = threading.Thread(target=read_gyr, name='read_gyr')
t1.daemon=True
t1.start()
t2 = threading.Thread(target=decode, name='decode')
t2.daemon=True
t2.start()
root.mainloop()
