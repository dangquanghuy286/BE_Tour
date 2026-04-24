package com.project.booktour.controllers;

import com.project.booktour.dtos.ContactDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.Contact;
import com.project.booktour.services.ContactService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @GetMapping
    public ResponseEntity<?> getAllContacts() {
        try {
            List<Contact> contacts = contactService.getAllContacts();
            return ResponseEntity.ok(contacts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách liên hệ: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getContactById(@PathVariable Integer id) {
        try {
            Contact contact = contactService.getContactById(id);
            return ResponseEntity.ok(contact);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin liên hệ: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createContact(
            @RequestBody @Valid ContactDTO contactDTO,
            BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(errorMessages);
            }

            Contact contact = convertToEntity(contactDTO);
            Contact savedContact = contactService.createContact(contact);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedContact);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo liên hệ: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateContact(
            @PathVariable Integer id,
            @RequestBody @Valid ContactDTO contactDTO,
            BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(errorMessages);
            }

            Contact contact = convertToEntity(contactDTO);
            Contact updatedContact = contactService.updateContact(id, contact);
            return ResponseEntity.ok(updatedContact);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật liên hệ: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContact(@PathVariable Integer id) {
        try {
            contactService.deleteContact(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Xóa liên hệ thành công");
            response.put("deletedId", id);
            return ResponseEntity.ok(response);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa liên hệ: " + e.getMessage());
        }
    }

    private Contact convertToEntity(ContactDTO dto) {
        Contact contact = new Contact();
        contact.setFullName(dto.getFullName());
        contact.setEmail(dto.getEmail());
        contact.setPhoneNumber(dto.getPhoneNumber());
        contact.setContent(dto.getContent());
        contact.setChecked(dto.isChecked());
        return contact;
    }
}