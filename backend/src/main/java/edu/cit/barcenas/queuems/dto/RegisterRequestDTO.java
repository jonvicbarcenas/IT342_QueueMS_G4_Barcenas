package edu.cit.barcenas.queuems.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String email;
    private String password;
    private String firstname;
    private String lastname;
}
