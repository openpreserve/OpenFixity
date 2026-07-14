# Acknowledgements

## Fixity Pro, and AVP

OpenFixity is the spiritual successor to **Fixity Pro**.

Fixity was first released by **AVP** (formerly AVPreserve) in 2013. Over the decade
that followed it became a mainstay of digital preservation practice, used by
universities, government agencies, memory institutions, and individual
practitioners to monitor and maintain the integrity of their collections.

AVP subsequently transferred ownership and intellectual property rights of Fixity
Pro to the **Open Preservation Foundation**, so that it could be made freely
available as open source software and its development continued. Registration,
subscription fees, and licence keys were retired at that point.

OpenFixity is **not a fork** of the original codebase. Fixity Pro had accumulated
over a decade of deprecated dependencies. The decision was taken to reimplement its
core, the file scanner and the checksum generator, on a modern Java stack,
deployable on a server or run locally, as Fixity Pro always was. What OpenFixity
inherits from Fixity Pro is its purpose, its users, and the problem it exists to
solve.

Our thanks to AVP for building the original, for sustaining it for a decade, and
for entrusting it to the community.

## Carl Wilson

**Carl Wilson** was Technical Lead at the Open Preservation Foundation, and the
technical lead on this project. He died on 21 June 2026.

Carl wrote the OpenFixity backend: the digest and path-scanning core, the
persistence layer, the scheduler, and the Dropwizard server that the web
application is built on. His name is on the source, in his `@author` tag at the top
of `OpenFixityServer.java`, and it is on the commits that carry his work into this
repository.

For close to two decades he oversaw OPF's technical work and was a driving force
behind its software, veraPDF and JHOVE among it. He was an open source engineer to
the core.

This repository reconstructs his work from the source archives we hold, committed
in sequence and under his authorship, so that it can be picked up and carried on.

Carl was an inspirational person who strove for excellence in everything he did,
both in his work and in his life. He was a lifelong advocate for the principles
that underpin the values and ideas of both open source and digital preservation.
Carl was a loyal friend and a dedicated colleague who will be missed by many; we
won't forget his keen wit or his incredible eye for detail.
