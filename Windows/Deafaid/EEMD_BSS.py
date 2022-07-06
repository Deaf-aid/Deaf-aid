from PyEMD import EEMD
import numpy as np
import matplotlib.pyplot as plt
from sklearn.decomposition import FastICA
import librosa
import scipy.signal as signal
import soundfile as sf

def signal_eemd(S, T):
    
    S = np.array(S)
    eemd = EEMD()

    E_IMFs = eemd.eemd(S, T)
    Num_IMFs = E_IMFs.shape[0]

    return E_IMFs, Num_IMFs

def fastica(mix, n):
    ica = FastICA(n_components=n)
    mix = mix.T
    u = ica.fit_transform(mix)
    u = u.T
    return u




