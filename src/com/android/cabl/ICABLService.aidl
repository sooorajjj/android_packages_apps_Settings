package com.android.cabl;

interface ICABLService{

 boolean startCABL() ;
 boolean stopCABL() ;
 boolean setCABLLevel(String level);
 void setCABLStateOnResume(boolean cablStatus);
}
