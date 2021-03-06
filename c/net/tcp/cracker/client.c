/*
 * Copyright 1997 SPY Internetworking
 *
 * $Id: client.c,v 1.4 2000/02/24 00:48:42 dustin Exp $
 */

#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <strings.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

#include "cracker.h"

/* The test */
#define test(p, t) (strcmp(p, crypt(t, p))==0)

void
finish()
{
	puts("Looks like somebody found the password");
	exit(0);
}

int
openhost(char *host)
{
	struct hostent *hp;
	register int s;
	struct sockaddr_in sin;

	if ((hp = gethostbyname(host)) == NULL) {
		printf("ERR: gethostbyname\n");
		exit(1);
	}
	if ((s = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
		perror("socket");
		exit(1);
	}
	sin.sin_family = AF_INET;
	sin.sin_port = htons(PORT);
	bcopy(hp->h_addr, &sin.sin_addr, hp->h_length);

	if (connect(s, (struct sockaddr *) &sin, sizeof(sin)) < 0) {
		perror("connect");
		exit(1);
	}
	return (s);
}

void
sendcommand(int s, struct command c)
{
	send(s, (char *) &c, sizeof(c), 0);
}

void
main(int argc, char **argv)
{
	int     s, i, tries = 0, tmp;
	struct command c;
	struct init ini;
	char    passwd[15];
	struct retpack r;
	char   *p;
	char pass_attempt[15];
	int passmap[8];

	signal(SIGPIPE, finish);
	s = openhost(argv[1]);

	c.command = htonl(COM_INIT);

	sendcommand(s, c);
	puts("Reading");
	recv(s, (char *) &ini, sizeof(ini), 0);

	strcpy(passwd, ini.password);

	puts(passwd);

	do {
		c.command = htonl(COM_GET);
		sendcommand(s, c);
		tmp = recv(s, (char *) &r, sizeof(r), 0);

		while (tmp < sizeof(r)) {
			printf("Only got %d bytes, should've been %d\n", tmp,
			    sizeof(r));
			p = (char *) &r + tmp;
			tmp += recv(s, p, sizeof(r) - tmp, 0);
		}

		set_pass(r.start, pass_attempt, passmap);

		/*
		 * printf("Trying packet %d\n", ++tries); 
		 */

		i=0;
		while ( (strcmp(r.stop, pass_attempt)!=0 ) && i<MAXPASS+1) {
			if (test(passwd, pass_attempt)) {
				c.command = htonl(COM_STOP);
				strcpy(c.passwd, pass_attempt);
				sendcommand(s, c);
				printf("FOUND IT!!!\n%s\n", pass_attempt);
				exit(0);
			}
			newpasswd(pass_attempt, passmap);
			i++;
		}
	}
	while (ntohl(r.info) == 0);

	printf("Exiting (ab)normally\n");
}
