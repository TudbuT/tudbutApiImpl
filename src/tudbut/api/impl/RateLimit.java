package tudbut.api.impl;

import tudbut.parsing.TCN;

public class RateLimit extends Exception {
    
    private final TCN theRateLimit;
    
    public RateLimit(TCN rateLimitObj) {
        super(rateLimitObj.getString("time") + rateLimitObj.getString("unit"));
        theRateLimit = rateLimitObj;
    }
    
    public TCN getRateLimitObject() {
        return theRateLimit;
    }
}
