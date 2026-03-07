package edu.cit.barcenas.queuems.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String uid;
    private String email;
    private String firstname;
    private String lastname;
    private String role = "USER"; // default
}
