package com.mk.contractservice.controllers.contract;

import com.mk.contractservice.controllers.contract.shared.InvalidExportCursorException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ContractExportCursor {

    private static final int UUID_BYTES = 16;

    static String encode(final UUID lastExportedId) {
        final ByteBuffer bytes = ByteBuffer.allocate(UUID_BYTES)
                .putLong(lastExportedId.getMostSignificantBits())
                .putLong(lastExportedId.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
    }

    static UUID decode(final String cursor) {
        final byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(cursor);
        } catch (IllegalArgumentException _) {
            throw new InvalidExportCursorException();
        }
        if (bytes.length != UUID_BYTES) {
            throw new InvalidExportCursorException();
        }
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
