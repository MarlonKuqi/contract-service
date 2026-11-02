package com.mk.contractservice.controllers.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ETag;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityTags {

    public static String of(final long version) {
        return new ETag(Long.toString(version), false).formattedTag();
    }

    public static long requiredVersion(@Nullable final String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionRequiredException("The If-Match header is required: send the ETag of the resource you read");
        }
        final List<ETag> tags = ETag.parse(ifMatch);
        if (tags.stream().anyMatch(ETag::isWildcard)) {
            throw new PreconditionRequiredException("If-Match: * is not accepted: send the ETag of the resource you read");
        }
        if (tags.size() != 1 || tags.getFirst().weak()) {
            throw new UnmatchableIfMatchException("If-Match must contain the single strong ETag of the resource you read");
        }
        try {
            return Long.parseLong(tags.getFirst().tag());
        } catch (final NumberFormatException e) {
            throw new UnmatchableIfMatchException("If-Match does not match any version of the resource");
        }
    }
}
