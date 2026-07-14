package com.kredio.backend.service;

import com.kredio.backend.entity.ClientReference;
import com.kredio.backend.repository.ClientReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientReferenceService {

    private final ClientReferenceRepository clientReferenceRepository;

    public List<ClientReference> getReferencesByClient(UUID clientId) {
        return clientReferenceRepository.findByClientId(clientId);
    }

    @Transactional
    public ClientReference createReference(ClientReference reference, UUID tenantId) {
        reference.setTenantId(tenantId);
        return clientReferenceRepository.save(reference);
    }

    @Transactional
    public ClientReference updateReference(UUID id, ClientReference referenceDetails) {
        ClientReference reference = clientReferenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referencia no encontrada"));

        reference.setFullName(referenceDetails.getFullName());
        reference.setPhone(referenceDetails.getPhone());
        reference.setRelationship(referenceDetails.getRelationship());
        reference.setAddress(referenceDetails.getAddress());

        return clientReferenceRepository.save(reference);
    }

    @Transactional
    public void deleteReference(UUID id) {
        ClientReference reference = clientReferenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referencia no encontrada"));
        clientReferenceRepository.delete(reference);
    }
}