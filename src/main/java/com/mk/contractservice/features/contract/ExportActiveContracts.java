package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.shared.UseCase;
import com.mk.contractservice.features.shared.PageBounds;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExportActiveContracts {

    int MAX_PAGE_SIZE = 1000;

    record Query(
            @Nullable UUID after,
            int size
    ) {
        public Query {
            PageBounds.requireValidSize(size, MAX_PAGE_SIZE);
        }
    }

    record Result(
            List<Contract> contracts,
            Optional<UUID> nextAfter
    ) {
    }

    Result execute(Query query);

    @UseCase
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements ExportActiveContracts {

        private final ContractRepository contractRepository;
        private final Clock clock;

        @Override
        public Result execute(final Query query) {
            final List<Contract> fetched = contractRepository.findActiveAfter(
                    LocalDateTime.now(clock),
                    query.after(),
                    query.size() + 1
            );
            if (fetched.size() <= query.size()) {
                return new Result(fetched, Optional.empty());
            }
            final List<Contract> page = fetched.subList(0, query.size());
            return new Result(page, Optional.ofNullable(page.getLast().getId()));
        }
    }
}
