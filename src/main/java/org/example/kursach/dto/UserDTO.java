package org.example.kursach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class UserDTO {
   @Null
   private final Long id;

   @NotNull
   private final String name;

   @NotNull
   @Email
   private final String email;

}
