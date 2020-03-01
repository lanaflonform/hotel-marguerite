package com.derteuffel.marguerite.services;

import com.derteuffel.marguerite.domain.Compte;
import com.derteuffel.marguerite.helpers.CompteRegistrationDto;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface CompteService extends UserDetailsService {
    Compte findByUsername(String username);
    Compte save(CompteRegistrationDto compteRegistrationDto, String s);
}
