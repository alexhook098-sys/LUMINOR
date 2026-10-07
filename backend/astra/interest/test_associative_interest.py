from associative_interest_engine import AssociativeInterestEngine


memories = [
    "старое окно",
    "тёплый свет",
    "далёкая музыка",
    "вечер",
]


engine = AssociativeInterestEngine(seed=42)

candidates = engine.generate(memories)

assert candidates
assert len(candidates) == 6

for candidate in candidates:
    assert len(candidate.source_items) == 2
    assert candidate.topic
    assert candidate.score > 0


selected = engine.select(memories)

assert selected is not None
assert len(selected.source_items) == 2


print("=" * 42)
print("ASTRA ASSOCIATIVE INTEREST ENGINE v0.2")
print("=" * 42)

print("\nMEMORIES:")

for item in memories:
    print("-", item)

print("\nASSOCIATIVE INTERESTS:")

for i, candidate in enumerate(candidates, 1):
    print(
        f"{i}. {candidate.topic}"
        f" | score={candidate.score:.3f}"
    )

print("\nSELECTED INTEREST:")
print(engine.explain(selected))

print("\nASSOCIATIVE INTEREST TEST: PASS")
print("=" * 42)
