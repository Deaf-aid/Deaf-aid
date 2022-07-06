from tkinter import *
import scipy.signal as signal
import pyaudio
import numpy as np
import copy
import scipy
t1 = 0.05

def sin_wave(f, time, biase, amp):
    samplerate = 48000
    t = np.arange(0, time, 1.0 / samplerate)
    signal_send = amp*(np.sin(2 * np.pi * f * t + biase)).astype(np.float32)
    return signal_send

def play_audio(sample):
    p = pyaudio.PyAudio()
    stream = p.open(format=pyaudio.paFloat32,
        channels=1,
        rate=48000,
        output=True)

    stream.write(sample)
    return p, stream

def stop_audio(p, stream):
    stream.stop_stream()
    stream.close()
    p.terminate()

def modulationAndsend(f, data, start_fre, stop_fre):
    f = float(f)
    zero = np.hstack((sin_wave(f, t1, 0, 1), sin_wave(f, t1*3, 0, 0)))
    one = np.hstack((sin_wave(f, t1, 0, 1), sin_wave(f, t1, 0, 0)))
    new_SOF = np.hstack((sin_wave(f, t1, 0, 1), sin_wave(f, t1*5, 0, 0),sin_wave(f, t1, 0, 1), sin_wave(f, t1, 0, 0)))
    new_EOF = np.hstack((sin_wave(f, t1, 0, 1), sin_wave(f, t1*7, 0, 0)))

    for begin_index in range(int(len(data)/128)):
        audio_stream = scipy.signal.chirp(0.5, start_fre, 0.5, stop_fre, method='linear', phi=0)
        audio_stream = np.hstack((audio_stream, audio_stream, audio_stream, copy.deepcopy(new_SOF)))
        data_tmp = data[begin_index*128: (begin_index+1)*128]
        data_send = []
        for i in range(len(data_tmp)):
            data_send += [data_tmp[i]-'0']
        data_send = np.array(data_send).reshape(8, 16)
        data_send = np.transpose(data_send, [1, 0])
        data_send = data_send.reshape(-1, 1)
        data_send = data_send[0]
        for i in range(128):
            if data_send[i] == 1:
                audio_stream = np.hstack((audio_stream, one))
            elif data_send[i] == 0:
                audio_stream = np.hstack((audio_stream, zero))
            else:
                txt.insert(END, "Input illegal character!")
                return
        
        audio_stream = np.hstack((audio_stream, new_EOF))
        play_audio(audio_stream)
    pass

root = Tk()
root.geometry('460x240')
root.title('sender')

lb1 = Label(root, text="tatget frequency:")
lb1.place(relx=0.02, rely=0.1, relwidth=0.3, relheight=0.1)
inp_fre = Entry(root)
inp_fre.place(relx=0.32, rely=0.1, relwidth=0.15, relheight=0.1)

lb3 = Label(root, text="start frequency:")
lb3.place(relx=0.02, rely=0.25, relwidth=0.3, relheight=0.1)
start_fre = Entry(root)
start_fre.place(relx=0.32, rely=0.25, relwidth=0.15, relheight=0.1)

lb4 = Label(root, text="stop frequency:")
lb4.place(relx=0.48, rely=0.25, relwidth=0.3, relheight=0.1)
stop_fre = Entry(root)
stop_fre.place(relx=0.78, rely=0.25, relwidth=0.15, relheight=0.1)

lb2 = Label(root, text="data:")
lb2.place(relx=0.05, rely=0.4, relwidth=0.15, relheight=0.1)
inp_data = Entry(root)
inp_data.place(relx=0.2, rely=0.4, relwidth=0.7, relheight=0.1)
send_btn = Button(root, text="send", command=lambda:modulationAndsend(inp_fre.get(), inp_data.get(), start_fre.get(), stop_fre.get()))
send_btn.place(relx=0.45, rely=0.6, relwidth=0.15, relheight=0.1)

txt = Text(root)
txt.place(rely=0.8, relheight=0.2)

root.mainloop()