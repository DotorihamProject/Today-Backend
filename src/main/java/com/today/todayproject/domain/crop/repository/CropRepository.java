package com.today.todayproject.domain.crop.repository;

import com.today.todayproject.domain.crop.Crop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CropRepository extends JpaRepository<Crop, Long>, CustomCropRepository {

    Optional<Crop> findByUserId(Long userId);

    Optional<List<Crop>> findAllByCreatedMonthAndUserId(int month, Long userId);

    int countByUserId(Long userId);
}
