E=smalleiffel
EC=$(E) compile

RPATH=-rpath /usr/local/pgsql/lib

EFLAGS=-g -I/usr/local/pgsql/include -debug_check
LFLAGS=-L/usr/local/pgsql/lib $(RPATH) -lpq
CSTUFF=/home/dustin/prog/eiffel/pg/pg_cstuff.c

mreplicate: mreplicate.e
	$(EC) $@ -o $@ $(EFLAGS) $(CSTUFF) $(LFLAGS)

clean:
	$(E) clean mreplicate
	rm -f mreplicate
