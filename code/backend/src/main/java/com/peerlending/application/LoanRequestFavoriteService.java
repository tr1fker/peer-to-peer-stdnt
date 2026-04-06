package com.peerlending.application;

import com.peerlending.api.dto.LoanRequestDto;
import com.peerlending.common.exception.NotFoundException;
import com.peerlending.persistence.entity.LoanRequestFavoriteEntity;
import com.peerlending.persistence.entity.LoanRequestEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.LoanRequestFavoriteRepository;
import com.peerlending.persistence.repository.LoanRequestRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoanRequestFavoriteService {

    private final LoanRequestFavoriteRepository favoriteRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final UserRepository userRepository;
    private final LoanRequestService loanRequestService;

    public LoanRequestFavoriteService(
            LoanRequestFavoriteRepository favoriteRepository,
            LoanRequestRepository loanRequestRepository,
            UserRepository userRepository,
            LoanRequestService loanRequestService
    ) {
        this.favoriteRepository = favoriteRepository;
        this.loanRequestRepository = loanRequestRepository;
        this.userRepository = userRepository;
        this.loanRequestService = loanRequestService;
    }

    @Transactional(readOnly = true)
    public List<LoanRequestDto> listFavorites(Long userId) {
        List<Long> ids = favoriteRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(f -> f.getLoanRequest().getId())
                .toList();
        return loanRequestService.listDtosForLoanRequestsInOrder(ids);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long loanRequestId) {
        return favoriteRepository.existsByUser_IdAndLoanRequest_Id(userId, loanRequestId);
    }

    @Transactional
    public void add(Long userId, Long loanRequestId) {
        loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new NotFoundException("Loan request not found"));
        if (favoriteRepository.existsByUser_IdAndLoanRequest_Id(userId, loanRequestId)) {
            return;
        }
        UserEntity user = userRepository.getReferenceById(userId);
        LoanRequestEntity lr = loanRequestRepository.getReferenceById(loanRequestId);
        LoanRequestFavoriteEntity e = new LoanRequestFavoriteEntity();
        e.setUser(user);
        e.setLoanRequest(lr);
        favoriteRepository.save(e);
    }

    @Transactional
    public void remove(Long userId, Long loanRequestId) {
        favoriteRepository.deleteByUser_IdAndLoanRequest_Id(userId, loanRequestId);
    }

    @Transactional(readOnly = true)
    public List<Long> listFavoriteLoanRequestIds(Long userId) {
        return favoriteRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(f -> f.getLoanRequest().getId())
                .toList();
    }
}
