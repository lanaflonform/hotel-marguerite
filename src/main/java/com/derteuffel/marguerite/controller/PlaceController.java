package com.derteuffel.marguerite.controller;

import com.derteuffel.marguerite.domain.Place;
import com.derteuffel.marguerite.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequestMapping("/hotel/places")
public class PlaceController {
    @Autowired
    private PlaceRepository placeRepository;

    @GetMapping("/all")
    public String findAll(Model model){
        model.addAttribute("places", placeRepository.findAll());
        return "places/all";
    }

    @GetMapping("/form")
    public String form(Model model){
        model.addAttribute("place", new Place());
        return "places/form";
    }

    @PostMapping("/save")
    public String save(@Valid Place place, RedirectAttributes redirectAttributes){

        if (place.getNumTable().contains("T")){
            place.setSecteur("TERRASSE");
        }else if (place.getNumTable().contains("L")){
            place.setSecteur("LOUNGE-BAR");
        }else {
            place.setSecteur("RESTAURANT");
        }
        placeRepository.save(place);
        redirectAttributes.addFlashAttribute("success", "You've been save your data successfully");
        return "redirect:/hotel/places/all";
    }

    @GetMapping("/edit/{id}")
    public String updatedPlace(Model model, @PathVariable Long id){
        Place place =  placeRepository.findById(id).get();
        model.addAttribute("place", place);
        return "places/edit";
    }

    @PostMapping("/update/{id}")
    public String save(@Valid Place place, @PathVariable("id") Long id,
                       BindingResult result, Model model,  String secteur, int nbrePlace, String numTable ){
        place.setSecteur(secteur);
        place.setNbrePlace(nbrePlace);
        place.setNumTable(numTable);
        placeRepository.save(place);
        model.addAttribute("places", placeRepository.findAll());
        return "redirect:/hotel/places/all";

    }

    @GetMapping("/delete/{id}")
    public String deleteById(@PathVariable Long id, Model model) {
        Place place =  placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid place id:" +id));
        System.out.println("place id: " + place.getSecteur());
        placeRepository.delete(place);
        model.addAttribute("places", placeRepository.findAll());
        return "redirect:/hotel/places/all" ;
    }

    @GetMapping("/detail/{id}")
    public String getTask(Model model, @PathVariable Long id){
        Place place =  placeRepository.findById(id).get();
        model.addAttribute("place", place);
        return "places/detail";

    }
}
