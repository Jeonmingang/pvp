package com.minkang.ultimate.pvpmm.util;
public class Elo { public static double expectedScore(double ra, double rb){ return 1.0 / (1.0 + Math.pow(10.0, (rb - ra) / 400.0)); } }
