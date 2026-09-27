package com.inhatc.demp.service;

import com.inhatc.demp.domain.Hashtag;
import com.inhatc.demp.repository.HashtagRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HashtagResolver {
    private final HashtagRepository hashtags;

    public List<Hashtag> resolve(List<String> requestedNames) {
        Set<String> names = new LinkedHashSet<>();
        if (requestedNames != null) {
            for (String rawName : requestedNames) {
                if (rawName != null && !rawName.trim().isEmpty()) names.add(rawName.trim());
            }
        }
        List<Hashtag> resolved = new ArrayList<>();
        for (String name : names) {
            resolved.add(hashtags.findByTagName(name)
                    .orElseGet(() -> hashtags.save(new Hashtag(name))));
        }
        return resolved;
    }
}
