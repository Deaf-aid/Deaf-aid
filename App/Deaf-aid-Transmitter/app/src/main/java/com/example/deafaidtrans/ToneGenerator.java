package com.example.deafaidtrans;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

class ToneGenerator {
    Thread t;
    int sr = 48000;
    double twopi = 2.* Math.PI;

    static double f1 = MainActivity.resonantFre;
    static double freq1 = MainActivity.leftFre;
    static double freq2 = (MainActivity.rightFre+MainActivity.leftFre)/2;
    static double f1_phi = 0;
    int[] msginput = convert(MainActivity.transmsg);

    public void startTone(){
        f1 = MainActivity.resonantFre;
        freq1 = MainActivity.leftFre;
        freq2 = (MainActivity.rightFre+MainActivity.leftFre)/2;

        t = new Thread(){
            public void run(){
                setPriority(Thread.MAX_PRIORITY);
                // creating the buffer
                int amp = (int) (10000);

                // generate a chirp
                double instfreq, numerator;
                int duration = 3;
                int sampleRate = sr;
                int numSample = duration * sampleRate;
                short[] frontsample = new short[numSample];

                for (int i = 0; i < numSample; i++)
                {
                    numerator = (double) (i%sampleRate)/(sampleRate);
                    instfreq = freq1 + (numerator*(freq2-freq1));
                    frontsample[i]= (short) (Math.sin(2*Math.PI*instfreq*(i%sampleRate)/sr)*amp);
                }

                int frameLen = (int) (0.5 * sr);
                // set symbols
                int[] msg0 = convert("100");
                short symbol0[] = new short[frameLen*msg0.length];
                for (int j = 0; j < msg0.length; j++) {
                    for (int i = 0; i < frameLen; i++) {
                        symbol0[i + j * frameLen] = (short) (amp * (Math.sin(f1_phi)));
                        f1_phi += twopi * f1 / sr * msg0[j];
                    }
                }

                int[] msg1 = convert("10");
                short symbol1[] = new short[frameLen*msg1.length];
                for (int j = 0; j < msg1.length; j++) {
                    for (int i = 0; i < frameLen; i++) {
                        symbol1[i + j * frameLen] = (short) (amp * (Math.sin(f1_phi)));
                        f1_phi += twopi * f1 / sr * msg1[j];
                    }
                }

                int[] msgSOF = convert("10000010");
                short symbolSOF[] = new short[frameLen*msgSOF.length];
                for (int j = 0; j < msgSOF.length; j++) {
                    for (int i = 0; i < frameLen; i++) {
                        symbolSOF[i + j * frameLen] = (short) (amp * (Math.sin(f1_phi)));
                        f1_phi += twopi * f1 / sr * msgSOF[j];
                    }
                }

                int[] msgEOF = convert("10000000");
                short symbolEOF[] = new short[frameLen*msgEOF.length];
                for (int j = 0; j < msgEOF.length; j++) {
                    for (int i = 0; i < frameLen; i++) {
                        symbolEOF[i + j * frameLen] = (short) (amp * (Math.sin(f1_phi)));
                        f1_phi += twopi * f1 / sr * msgEOF[j];
                    }
                }

                // set transmitted symbols
                short data[] = symbolSOF;
                short temp[];
                for (int i = 0; i < msginput.length; i++) {
                    if (msginput[i] == 1)
                    {
                        temp = new short[data.length + symbol1.length];
                        System.arraycopy(data,0, temp,0, data.length);
                        System.arraycopy(symbol1,0, temp, data.length, symbol1.length);
                    } else {
                        temp = new short[data.length + symbol0.length];
                        System.arraycopy(data,0, temp,0, data.length);
                        System.arraycopy(symbol0,0, temp, data.length, symbol0.length);
                    }
                    data = temp;

                }
                temp = new short[data.length + symbolEOF.length];
                System.arraycopy(data,0, temp,0, data.length);
                System.arraycopy(symbolEOF,0, temp, data.length, symbolEOF.length);
                data = temp;


                temp = new short[data.length + frontsample.length];
                System.arraycopy(frontsample,0, temp,0, frontsample.length);
                System.arraycopy(data,0, temp, frontsample.length, data.length);
                data = temp;


                // create audio object
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC, sr,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, data.length*2,
                        AudioTrack.MODE_STATIC);

                audioTrack.write(data, 0, data.length);
                Log.d("AudioSize",String.valueOf((float) data.length/sr));
                audioTrack.play();
            }
        };
        t.start();
    }

    public void stopTone(){
        try{
            t.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        t = null;
    }

    private int[] convert(String str) {
        int number[] = new int[str.length()];

        for (int i = 0; i < str.length(); i++) {
            number[i] = Integer.parseInt(String.valueOf(str.charAt(i)));
        }
        return number;
    }

}
