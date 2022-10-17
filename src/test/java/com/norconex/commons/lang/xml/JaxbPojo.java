package com.norconex.commons.lang.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@XmlRootElement
@Data
public class JaxbPojo {

    private String lastName;
    private String firstName;
    private int luckyNumber = 7;

    @XmlAttribute
    public void setLuckyNumber(int luckyNumber) {
        this.luckyNumber = luckyNumber;
    }
}
