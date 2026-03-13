/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notification.usecases.DeleteNewsFeedItemUseCase;
import com.akademiaplus.notification.usecases.GetAllNewsFeedItemsUseCase;
import com.akademiaplus.notification.usecases.GetNewsFeedItemByIdUseCase;
import com.akademiaplus.notification.usecases.NewsFeedItemCreationUseCase;
import com.akademiaplus.notification.usecases.UpdateNewsFeedItemUseCase;
import openapi.akademiaplus.domain.notification.system.api.NewsFeedApi;
import openapi.akademiaplus.domain.notification.system.dto.GetNewsFeedItemResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for news feed item management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/notification-system")
public class NewsFeedItemController implements NewsFeedApi {

    private final NewsFeedItemCreationUseCase newsFeedItemCreationUseCase;
    private final GetAllNewsFeedItemsUseCase getAllNewsFeedItemsUseCase;
    private final GetNewsFeedItemByIdUseCase getNewsFeedItemByIdUseCase;
    private final UpdateNewsFeedItemUseCase updateNewsFeedItemUseCase;
    private final DeleteNewsFeedItemUseCase deleteNewsFeedItemUseCase;

    public NewsFeedItemController(NewsFeedItemCreationUseCase newsFeedItemCreationUseCase,
                                  GetAllNewsFeedItemsUseCase getAllNewsFeedItemsUseCase,
                                  GetNewsFeedItemByIdUseCase getNewsFeedItemByIdUseCase,
                                  UpdateNewsFeedItemUseCase updateNewsFeedItemUseCase,
                                  DeleteNewsFeedItemUseCase deleteNewsFeedItemUseCase) {
        this.newsFeedItemCreationUseCase = newsFeedItemCreationUseCase;
        this.getAllNewsFeedItemsUseCase = getAllNewsFeedItemsUseCase;
        this.getNewsFeedItemByIdUseCase = getNewsFeedItemByIdUseCase;
        this.updateNewsFeedItemUseCase = updateNewsFeedItemUseCase;
        this.deleteNewsFeedItemUseCase = deleteNewsFeedItemUseCase;
    }

    @Override
    public ResponseEntity<NewsFeedItemCreationResponseDTO> createNewsFeedItem(
            NewsFeedItemCreationRequestDTO newsFeedItemCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(newsFeedItemCreationUseCase.create(newsFeedItemCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetNewsFeedItemResponseDTO>> getNewsFeedItems(
            Long courseId, Integer page, Integer size) {
        return ResponseEntity.ok(getAllNewsFeedItemsUseCase.getAll(courseId));
    }

    @Override
    public ResponseEntity<GetNewsFeedItemResponseDTO> getNewsFeedItemById(Long newsFeedItemId) {
        return ResponseEntity.ok(getNewsFeedItemByIdUseCase.get(newsFeedItemId));
    }

    @Override
    public ResponseEntity<NewsFeedItemCreationResponseDTO> updateNewsFeedItem(
            Long newsFeedItemId,
            NewsFeedItemCreationRequestDTO newsFeedItemCreationRequestDTO) {
        return ResponseEntity.ok(
                updateNewsFeedItemUseCase.update(newsFeedItemId, newsFeedItemCreationRequestDTO));
    }

    @Override
    public ResponseEntity<Void> deleteNewsFeedItem(Long newsFeedItemId) {
        deleteNewsFeedItemUseCase.delete(newsFeedItemId);
        return ResponseEntity.noContent().build();
    }
}
