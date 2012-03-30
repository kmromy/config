package com.typesafe.config.impl;

import com.typesafe.config.impl.AbstractConfigValue.NotPossibleToResolve;

/**
 * This class is the source for values for a substitution like ${foo}.
 */
final class ResolveSource {
    final private AbstractConfigObject root;

    ResolveSource(AbstractConfigObject root) {
        this.root = root;
    }

    /** resolver is null if we should not have refs */
    static private AbstractConfigValue findInObject(final AbstractConfigObject obj,
            final SubstitutionResolver resolver, final ResolveContext context,
            ConfigSubstitution traversed, final SubstitutionExpression subst)
            throws NotPossibleToResolve {
        return context.traversing(traversed, subst, new ResolveContext.Resolver() {
            @Override
            public AbstractConfigValue call() throws NotPossibleToResolve {
                return obj.peekPath(subst.path(), resolver, context);
            }
        });
    }

    AbstractConfigValue lookupSubst(final SubstitutionResolver resolver,
            final ResolveContext context, ConfigSubstitution traversed,
            final SubstitutionExpression subst, int prefixLength) throws NotPossibleToResolve {
        // First we look up the full path, which means relative to the
        // included file if we were not a root file
        AbstractConfigValue result = findInObject(root, resolver, context, traversed, subst);

        if (result == null) {
            // Then we want to check relative to the root file. We don't
            // want the prefix we were included at to be used when looking
            // up env variables either.
            SubstitutionExpression unprefixed = subst
                    .changePath(subst.path().subPath(prefixLength));

            if (result == null && prefixLength > 0) {
                result = findInObject(root, resolver, context, traversed, unprefixed);
            }

            if (result == null && context.options().getUseSystemEnvironment()) {
                result = findInObject(ConfigImpl.envVariablesAsConfigObject(), null, context,
                        traversed, unprefixed);
            }
        }

        if (result != null) {
            final AbstractConfigValue unresolved = result;
            result = context.traversing(traversed, subst, new ResolveContext.Resolver() {
                @Override
                public AbstractConfigValue call() throws NotPossibleToResolve {
                    return resolver.resolve(unresolved, context);
                }
            });
        }

        return result;
    }
}