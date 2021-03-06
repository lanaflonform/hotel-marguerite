package com.derteuffel.marguerite.controller;

import com.derteuffel.marguerite.domain.Chambre;
import com.derteuffel.marguerite.domain.Compte;
import com.derteuffel.marguerite.domain.Facture;
import com.derteuffel.marguerite.domain.Reservation;
import com.derteuffel.marguerite.repository.ChambreRepository;
import com.derteuffel.marguerite.repository.ReservationRepository;
import com.derteuffel.marguerite.repository.RoleRepository;
import com.derteuffel.marguerite.services.CompteService;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Controller
@RequestMapping("/hotel/reservations")
public class ReservationController {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ChambreRepository chambreRepository;
    @Autowired
    private CompteService compteService;

    @Autowired
    private RoleRepository roleRepository;
    @Value("${file.upload-dir}")
    private String fileStorage;

    @GetMapping("/all")
    public String findAll(Model model, HttpServletRequest request){
        Principal principal = request.getUserPrincipal();
        Compte compte = compteService.findByUsername(principal.getName());
        model.addAttribute("reservations", reservationRepository.findAll());
        if (compte.getRoles().size() <= 1 && compte.getRoles().contains(roleRepository.findByName("SELLER"))){
            return "redirect:/hotel/reservations/reservation";
        }else {
            return "reservations/reservations";
        }
    }

    @GetMapping("/reservation")
    public String getAllByStatus(Model model) {
        List<Reservation>reservations = reservationRepository.findAll(Sort.by(Sort.Direction.DESC,"id"));
        model.addAttribute("reservations", reservations);
        return "reservations/all";
    }

    @GetMapping("/form/{id}")
    public String form(Model model, @PathVariable Long id){
        Chambre chambre = chambreRepository.getOne(id);
        model.addAttribute("chambre",chambre);
        model.addAttribute("reservation", new Reservation());
        return "reservations/form";
    }

    @PostMapping("/save")
    public String save(@Valid Reservation reservation, String num, HttpServletRequest request, Model model){
        Principal principal = request.getUserPrincipal();
        Compte compte = compteService.findByUsername(principal.getName());
        System.out.println(num);
        Chambre chambre = chambreRepository.findByNumero(num);
        reservation.setPrixT(reservation.getPrixU() * reservation.getNbreNuits());
        if (chambre != null){
            reservation.setChambre(chambre);
            TimerTask activate = new TimerTask() {
                @Override
                public void run() {
                    chambre.setStatus(true);
                    reservation.setStatus(true);
                    System.out.println("action started");
                }
            };
            TimerTask deactivate = new TimerTask() {
                @Override
                public void run() {
                    chambre.setStatus(false);
                    reservation.setStatus(false);
                }
            };
            Timer timer = new Timer();
            timer.schedule(activate,reservation.getDateDebut());
            timer.schedule(deactivate,reservation.getDateFin());
            reservation.setCompte(compte);
            reservation.setNumReservation((reservation.getChambre().getNumero()+(reservationRepository.findAll().size()+1)).toUpperCase());
            reservationRepository.save(reservation);
            return "redirect:/hotel/reservations/detail/"+reservation.getId();
        }else {
            model.addAttribute("error", "There are no room with the provided number :"+num);
            return "reservations/form";
        }

    }

    @GetMapping("/edit/{id}")
    public String updatedReservation(Model model, @PathVariable Long id){
        Reservation reservation =  reservationRepository.findById(id).get();
        model.addAttribute("reservation", reservation);
        return "reservations/edit";
    }

    @PostMapping("/update")
    public String update(@Valid Reservation reservation, String num, HttpServletRequest request, Model model){
        Principal principal = request.getUserPrincipal();
        Compte compte = compteService.findByUsername(principal.getName());
        Chambre chambre = chambreRepository.findByNumero(num);
        reservation.setPrixT(reservation.getPrixU() * reservation.getNbreNuits());
        if (chambre != null){
            reservation.setChambre(chambre);
            reservation.setCompte(compte);
            reservationRepository.save(reservation);
            return "redirect:/hotel/reservations/all";
        }else {
            model.addAttribute("reservation",reservationRepository.getOne(reservation.getId()));
            model.addAttribute("error", "There are no room with the provided number :"+num);
            return "reservations/edit";
        }

    }

    @GetMapping("/delete/{id}")
    public String deleteById(@PathVariable Long id, Model model, HttpSession session) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reservation id:" +id));
        System.out.println("reservation id: " + reservation.getId());
        reservationRepository.delete(reservation);
        return "redirect:/hotel/reservations/all" ;
    }

    @GetMapping("/detail/{id}")
    public String getReservation(Model model, @PathVariable Long id){
        Reservation reservation = reservationRepository.findById(id).get();
        model.addAttribute("reservation", reservation);
        return "reservations/detail";

    }


    @GetMapping("/pdf/generate/{id}")
    public String reservationBill(@PathVariable Long id, Model model){
        Reservation reservation = reservationRepository.getOne(id);
        Document document = new Document();
        try{
            PdfWriter.getInstance(document,new FileOutputStream(new File((fileStorage+"CH"+reservation.getId()+".pdf").toString())));
            document.open();
            document.add(new Paragraph("Marguerite Hotel"));
            document.add(new Paragraph("Secteur :   "+"Resrvation chambre"));
            document.add(new Paragraph("Reservation Numero :   "+reservation.getNumReservation()));
            document.add(new Paragraph("Chambre Numero :  "+reservation.getChambre().getNumero()));
            document.add(new Paragraph("Date du jour :  "+reservation.getDateJour()));
            document.add(new Paragraph("Nom client :  "+reservation.getNomClient()));
            document.add(new Paragraph("Telephone client :  "+reservation.getTelephone()));
            document.add(new Paragraph("Email client :  "+reservation.getEmail()));
            document.add(new Paragraph("Nombre de nuite :  "+reservation.getNbreNuits()));
            document.add(new Paragraph("Debut du sejour :  "+reservation.getDateDebut()));
            document.add(new Paragraph("Fin du sejour :  "+reservation.getDateFin()));
            document.add(new Paragraph("Cout du sejour :  "+reservation.getPrixT()));
            document.add(new Paragraph("Liste des articles et quantites "));
            document.close();
            System.out.println("the job is done!!!");
            reservation.setBillTrace("/downloadFile/"+"CH"+reservation.getId()+".pdf");
            reservationRepository.save(reservation);
        } catch (FileNotFoundException | DocumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/hotel/reservations/detail/"+reservation.getId();
    }
}
