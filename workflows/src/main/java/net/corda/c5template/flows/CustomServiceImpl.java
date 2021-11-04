package net.corda.c5template.flows;

class CustomServiceImpl implements CustomService {
    public CustomServiceImpl(){
        System.out.println("CustomServiceImpl");
    }

    public void fun(){
        System.out.println("***********from Custom service***********");
    }
}
