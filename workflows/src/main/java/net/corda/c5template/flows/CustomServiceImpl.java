package net.corda.c5template.service;

class CustomServiceImpl implements CustomService {
    public CustomServiceImpl(){
        System.out.println("CustomServiceImpl");
    }

    public void fun(){
        System.out.println("***********from Custom service***********");
    }
}
