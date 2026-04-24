package com.project.booktour.services;

import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.Contact;
import com.project.booktour.repositories.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    @Autowired
    private ContactRepository contactRepository;

    public Contact createContact(Contact contact) {
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());
        Contact savedContact = contactRepository.save(contact);

        return savedContact;
    }

    public List<Contact> getAllContacts() {
        logger.info("Retrieving all contacts");
        return contactRepository.findAll();
    }

    public Contact getContactById(Integer id) throws DataNotFoundException {
        return contactRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy liên hệ với ID: " + id));
    }

    public Contact updateContact(Integer id, Contact updatedContact) throws DataNotFoundException {
        Contact existingContact = contactRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy liên hệ với ID: " + id));

        existingContact.setFullName(updatedContact.getFullName());
        existingContact.setEmail(updatedContact.getEmail());
        existingContact.setPhoneNumber(updatedContact.getPhoneNumber());
        existingContact.setContent(updatedContact.getContent());
        existingContact.setChecked(updatedContact.isChecked());
        existingContact.setUpdatedAt(LocalDateTime.now());

        logger.info("Updated contact with ID: {}", id);
        return contactRepository.save(existingContact);
    }

    public void deleteContact(Integer id) throws DataNotFoundException {
        if (!contactRepository.existsById(id)) {
            throw new DataNotFoundException("Không tìm thấy liên hệ với ID: " + id);
        }
        contactRepository.deleteById(id);
        logger.info("Deleted contact with ID: {}", id);
    }
}