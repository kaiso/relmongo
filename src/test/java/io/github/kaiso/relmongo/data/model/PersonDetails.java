package io.github.kaiso.relmongo.data.model;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.OneToOne;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "detail")
public class PersonDetails {

    @Id
    private ObjectId id;
    private String bank;

    @OneToOne(mappedBy = "personDetails", fetch = FetchType.LAZY)
    private Person owner;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

}