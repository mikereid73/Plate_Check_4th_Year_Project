package c00112726.itcarlow.ie.finalyearproject.misc;

import java.io.Serializable;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 01/04/2016
 */
public class NumberPlate implements Serializable {

    private String year;
    private String county;
    private String reg;
    private String wrongGuess;

    public NumberPlate(String year, String county, String reg) {
        this.year = year;
        this.county = county;
        this.reg = reg;

        wrongGuess = year + county + reg;
    }

    public String getWrongGuess() {
        return wrongGuess;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getReg() {
        return reg;
    }

    public void setReg(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
        return year + county + reg;
    }
}

