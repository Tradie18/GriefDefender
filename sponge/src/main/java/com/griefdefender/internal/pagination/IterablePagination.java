/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.internal.pagination;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.griefdefender.command.CommandException;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import org.spongepowered.api.command.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Pagination occurring for an iterable -- we don't know its size.
 */
class IterablePagination extends ActivePagination {

    private final PeekingIterator<Map.Entry<Component, Integer>> countIterator;
    private int lastPage;

    public IterablePagination(Supplier<Optional<CommandSource>> src, GDPaginationCalculator calc, Iterable<Map.Entry<Component, Integer>> counts,
            @Nullable Component title, @Nullable Component header, @Nullable Component footer, Component padding) {
        super(src, calc, title, header, footer, padding);
        this.countIterator = Iterators.peekingIterator(counts.iterator());
    }

    @Override
    protected Iterable<Component> getLines(int page) throws CommandException {
        if (!this.countIterator.hasNext()) {
            throw new CommandException(TextComponent.of("You're already at the end of the pagination list iterator."));
        }

        if (page < 1) {
            throw new CommandException(TextComponent.of(String.format("Page %s does not exist!", page)));
        }

        if (page <= this.lastPage) {
            throw new CommandException(TextComponent.of("You cannot go to previous pages in an iterable pagination."));
        } else if (page > this.lastPage + 1) {
            getLines(page - 1);
        }
        this.lastPage = page;

        if (getMaxContentLinesPerPage() <= 0) {
            return Lists.newArrayList(Iterators.transform(this.countIterator, new Function<Map.Entry<Component, Integer>, Component>() {

                @Nullable
                @Override
                public Component apply(Map.Entry<Component, Integer> input) {
                    return input.getKey();
                }
            }));
        }

        List<Component> ret = new ArrayList<>(getMaxContentLinesPerPage());
        int addedLines = 0;
        while (addedLines <= getMaxContentLinesPerPage()) {
            if (!this.countIterator.hasNext()) {
                // Pad the last page, but only if it isn't the first.
                if (page > 1) {
                    padPage(ret, addedLines, false);
                }
                break;
            }
            if (addedLines + this.countIterator.peek().getValue() > getMaxContentLinesPerPage()) {
                // Add the continuation marker, pad if required
                padPage(ret, addedLines, true);
                break;
            }
            Map.Entry<Component, Integer> ent = this.countIterator.next();
            ret.add(ent.getKey());
            addedLines += ent.getValue();
        }
        return ret;
    }

    @Override
    protected boolean hasPrevious(int page) {
        return false;
    }

    @Override
    protected boolean hasNext(int page) {
        return page == getCurrentPage() && this.countIterator.hasNext();
    }

    @Override
    protected int getTotalPages() {
        return -1;
    }

    @Override
    public void previousPage() throws CommandException {
        throw new CommandException(TextComponent.of("You cannot go to previous pages in an iterable pagination."));
    }
}
