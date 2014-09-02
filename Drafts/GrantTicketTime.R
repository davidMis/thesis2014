t<-read.csv("ticketTiming.txt", header=FALSE)
pdf("GrantTicketTime.pdf", width=4.5, height=4, useDingbats=FALSE )
plot(t$V2, type="l", xlab="Days worth of tickets generated", ylab="Minutes", axes=FALSE, xlim=c(0,30), ylim=c(0,8*60000))
lines(t$V1)
axis(2, at=c(0,1*60000,2*60000,3*60000,4*60000, 5*60000, 6*60000,7*60000,8*60000), c(0,1,2,3,4,5,6,7,8)) 
axis(1, at=c(0,5,10,15,20,25,30), c(0,5,10,15,20,25,30))
text(x=c(20,23), y=c(7*60000,3.3*60000), labels = c("Total time", "Phone calculation time"))
dev.off()

