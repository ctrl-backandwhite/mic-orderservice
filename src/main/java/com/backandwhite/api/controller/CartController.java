package com.backandwhite.api.controller;

import com.backandwhite.api.dto.in.CartItemDtoIn;
import com.backandwhite.api.dto.in.CartItemQuantityDtoIn;
import com.backandwhite.api.dto.in.CartMergeDtoIn;
import com.backandwhite.api.dto.out.CartDtoOut;
import com.backandwhite.api.dto.out.CartItemDtoOut;
import com.backandwhite.api.mapper.CartApiMapper;
import com.backandwhite.application.usecase.CartUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Endpointsparagestióndelcarritodecompras")
public class CartController {
    private final CartUseCase cartUseCase;
    private final CartApiMapper cartApiMapper;

    @GetMapping
    @Operation(summary = "Obtenercarritoactivo", description = "Devuelveelcarritoactivodelusuarioosesión")
    public ResponseEntity<CartDtoOut> getActiveCart(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader(value = "X-Auth-Subject", required = false) String userId,
            @Parameter(description = "SessionID") @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        Cart cart = cartUseCase.getActiveCart(userId, sessionId);
        return ResponseEntity.ok(cartApiMapper.toDto(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Agregaritemalcarrito", description = "Agregaunproductoalcarrito.Siyaexiste,incrementalacantidad")
    public ResponseEntity<CartDtoOut> addItem(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader(value = "X-Auth-Subject", required = false) String userId,
            @Parameter(description = "SessionID") @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody CartItemDtoIn dto) {
        CartItem item = cartApiMapper.toDomain(dto);
        Cart cart = cartUseCase.addItem(userId, sessionId, item);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartApiMapper.toDto(cart));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Actualizarcantidaddeitem", description = "Actualizalacantidaddeunitemenelcarrito")
    public ResponseEntity<CartDtoOut> updateItemQuantity(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelitem") @PathVariable String itemId,
            @Valid @RequestBody CartItemQuantityDtoIn dto) {
        Cart cart = cartUseCase.updateItemQuantity(itemId, dto.getQuantity());
        return ResponseEntity.ok(cartApiMapper.toDto(cart));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Eliminaritemdelcarrito")
    public ResponseEntity<CartDtoOut> removeItem(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader(value = "X-Auth-Subject", required = false) String userId,
            @Parameter(description = "SessionID") @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Parameter(description = "IDdelitem") @PathVariable String itemId) {
        Cart cart = cartUseCase.removeItem(userId, sessionId, itemId);
        return ResponseEntity.ok(cartApiMapper.toDto(cart));
    }

    @DeleteMapping
    @Operation(summary = "Vaciarcarrito", description = "Eliminatodoslositemsdelcarrito")
    public ResponseEntity<Void> clearCart(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader(value = "X-Auth-Subject", required = false) String userId,
            @Parameter(description = "SessionID") @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        cartUseCase.clearCart(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    @Operation(summary = "Fusionarcarrito", description = "Fusionaelcarritoanónimo (sessionId)coneldelusuarioautenticado")
    public ResponseEntity<CartDtoOut> mergeCart(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader("X-Auth-Subject") String userId,
            @Valid @RequestBody CartMergeDtoIn dto) {
        Cart merged = cartUseCase.mergeCart(userId, dto.getSessionId());
        return ResponseEntity.ok(cartApiMapper.toDto(merged));
    }
}
